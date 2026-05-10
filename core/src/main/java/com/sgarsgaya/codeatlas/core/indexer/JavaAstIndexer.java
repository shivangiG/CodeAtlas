package com.sgarsgaya.codeatlas.core.indexer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sgarsgaya.codeatlas.core.model.Confidence;
import com.sgarsgaya.codeatlas.core.model.EdgeKind;
import com.sgarsgaya.codeatlas.core.model.EvidenceSource;
import com.sgarsgaya.codeatlas.core.model.GraphNode;
import com.sgarsgaya.codeatlas.core.model.NodeKind;
import com.sgarsgaya.codeatlas.core.model.SourceRange;
import com.sgarsgaya.codeatlas.core.model.SymbolIdentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** JavaParser + SymbolSolver extraction of declarations and best-effort call edges. */
public final class JavaAstIndexer {

    private static final Logger log = LoggerFactory.getLogger(JavaAstIndexer.class);

    private final String snapshotId;
    private final Path repoRoot;
    private final JavaParser javaParser;
    private final Set<String> declaredNodeIds = new HashSet<>();

    public JavaAstIndexer(String snapshotId, ScanResult scanResult) {
        this.snapshotId = Objects.requireNonNull(snapshotId);
        this.repoRoot = scanResult.repoRoot().toAbsolutePath().normalize();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        for (Path root : scanResult.modules().stream()
                .flatMap(m -> m.sourceRoots().stream())
                .distinct()
                .toList()) {
            if (Files.isDirectory(root)) {
                combinedTypeSolver.add(new JavaParserTypeSolver(root));
            }
        }

        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        this.javaParser = new JavaParser(configuration);
    }

    public static IndexedSymbols indexAll(String snapshotId, ScanResult scanResult) {
        JavaAstIndexer indexer = new JavaAstIndexer(snapshotId, scanResult);
        IndexedSymbols out = new IndexedSymbols();
        for (SourceFile sourceFile : scanResult.sourceFiles()) {
            try {
                indexer.indexFile(sourceFile, out);
            } catch (IOException e) {
                out.addDiagnostic(new IndexDiagnostic(
                        posixKey(sourceFile.relativePath()),
                        "Failed to read/parse: " + e.getMessage(),
                        DiagnosticSeverity.WARNING));
            }
        }
        return out;
    }

    private void indexFile(SourceFile sourceFile, IndexedSymbols out) throws IOException {
        Path path = sourceFile.absolutePath();
        var parseResult = javaParser.parse(path);
        Optional<CompilationUnit> opt = parseResult.getResult();
        if (opt.isEmpty()) {
            parseResult.getProblems().forEach(p -> out.addDiagnostic(new IndexDiagnostic(
                    posixKey(sourceFile.relativePath()), p.toString(), DiagnosticSeverity.WARNING)));
            return;
        }
        CompilationUnit cu = opt.orElseThrow();

        String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        String relPosix = posixKey(sourceFile.relativePath());
        String fileId = "file:" + relPosix;
        String pkgId = packageName.isEmpty() ? "pkg:default" : "pkg:" + packageName;

        ensureNode(
                out,
                new GraphNode(pkgId, NodeKind.PACKAGE, packageName, null, null, null, "{}", snapshotId));

        JsonObject fileMeta = new JsonObject();
        fileMeta.add("annotations", new JsonArray());
        ensureNode(
                out,
                new GraphNode(
                        fileId,
                        NodeKind.FILE,
                        relPosix,
                        null,
                        relPosix,
                        null,
                        fileMeta.toString(),
                        snapshotId));

        for (ImportDeclaration imp : cu.getImports()) {
            String imported = imp.getNameAsString();
            String importedId = imported.endsWith("*") ? ("pkg:" + imported.replace(".*", "")) : ("type:" + imported);
            NodeKind nk = imported.endsWith("*") ? NodeKind.PACKAGE : NodeKind.CLASS;
            ensureNode(
                    out,
                    new GraphNode(importedId, nk, imported, null, null, null, "{}", snapshotId));

            out.addEdgeCandidate(
                    EdgeCandidate.builder(fileId, importedId, EdgeKind.IMPORTS, relPosix)
                            .evidenceSource(EvidenceSource.NAME_MATCH)
                            .confidence(Confidence.LOW)
                            .sourceRange(EdgeCandidate.openapiSyntheticRange())
                            .reason("java_import")
                            .build());
        }

        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            indexType(sourceFile, cu, type, out);
        }
    }

    private void indexType(
            SourceFile sourceFile, CompilationUnit cu, ClassOrInterfaceDeclaration type, IndexedSymbols out) {

        String fqName = type.getFullyQualifiedName().orElseGet(() -> guessFqName(cu, type.getNameAsString()));
        String typeId = "type:" + fqName;
        NodeKind typeKind = type.isInterface() ? NodeKind.INTERFACE : NodeKind.CLASS;
        String relPosix = posixKey(sourceFile.relativePath());

        JsonObject meta = new JsonObject();
        JsonArray anns = new JsonArray();
        JsonArray details = new JsonArray();
        for (AnnotationExpr ann : type.getAnnotations()) {
            anns.add(ann.getNameAsString());
            details.add(annotationToJson(ann));
        }
        meta.add("annotations", anns);
        meta.add("springDetails", details);

        ensureNode(
                out,
                new GraphNode(
                        typeId,
                        typeKind,
                        fqName,
                        null,
                        relPosix,
                        rangeOf(type),
                        meta.toString(),
                        snapshotId));

        String fileId = "file:" + relPosix;
        out.addEdgeCandidate(
                EdgeCandidate.builder(fileId, typeId, EdgeKind.CONTAINS, relPosix)
                        .evidenceSource(EvidenceSource.SOLVER)
                        .confidence(Confidence.HIGH)
                        .sourceRange(rangeOf(type))
                        .reason("file_contains_type")
                        .build());

        String pkgName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        String pkgId = pkgName.isEmpty() ? "pkg:default" : "pkg:" + pkgName;
        out.addEdgeCandidate(
                EdgeCandidate.builder(pkgId, typeId, EdgeKind.DECLARES, relPosix)
                        .evidenceSource(EvidenceSource.SOLVER)
                        .confidence(Confidence.HIGH)
                        .sourceRange(rangeOf(type))
                        .reason("package_declares_type")
                        .build());

        for (MethodDeclaration method : type.getMethods()) {
            indexMethod(sourceFile, fqName, typeId, method, out);
        }

        for (FieldDeclaration field : type.getFields()) {
            for (var v : field.getVariables()) {
                indexField(sourceFile, fqName, typeId, field, v.getNameAsString(), out);
            }
        }
    }

    private void indexField(
            SourceFile sourceFile,
            String fqClass,
            String typeId,
            FieldDeclaration field,
            String name,
            IndexedSymbols out) {

        String fieldSig = fqClass + "#" + name;
        String fieldId = "fld:" + fieldSig;
        String relPosix = posixKey(sourceFile.relativePath());
        ensureNode(
                out,
                new GraphNode(
                        fieldId,
                        NodeKind.FIELD,
                        fieldSig,
                        null,
                        relPosix,
                        rangeOf(field),
                        "{}",
                        snapshotId));

        out.addEdgeCandidate(
                EdgeCandidate.builder(typeId, fieldId, EdgeKind.DECLARES, relPosix)
                        .evidenceSource(EvidenceSource.SOLVER)
                        .confidence(Confidence.HIGH)
                        .sourceRange(rangeOf(field))
                        .reason("type_declares_field")
                        .build());
    }

    private void indexMethod(
            SourceFile sourceFile, String fqClass, String typeId, MethodDeclaration method, IndexedSymbols out) {

        String params = String.join(
                ",", method.getParameters().stream().map(p -> p.getType().asString()).toList());
        String ret = method.getType().asString();
        String primary = SymbolIdentity.primary(fqClass, method.getNameAsString(), params, ret);
        String methodId = "meth:" + primary;
        String relPosix = posixKey(sourceFile.relativePath());

        JsonObject meta = new JsonObject();
        JsonArray anns = new JsonArray();
        JsonArray details = new JsonArray();
        for (AnnotationExpr ann : method.getAnnotations()) {
            anns.add(ann.getNameAsString());
            details.add(annotationToJson(ann));
        }
        meta.add("annotations", anns);
        meta.add("springDetails", details);

        ensureNode(
                out,
                new GraphNode(
                        methodId,
                        NodeKind.METHOD,
                        primary,
                        null,
                        relPosix,
                        rangeOf(method),
                        meta.toString(),
                        snapshotId));

        out.addEdgeCandidate(
                EdgeCandidate.builder(typeId, methodId, EdgeKind.DECLARES, relPosix)
                        .evidenceSource(EvidenceSource.SOLVER)
                        .confidence(Confidence.HIGH)
                        .sourceRange(rangeOf(method))
                        .reason("type_declares_method")
                        .build());

        method.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodCallExpr n, Void arg) {
                        super.visit(n, arg);
                        handleCall(sourceFile, methodId, n, out);
                    }
                },
                null);
    }

    private void handleCall(SourceFile sourceFile, String callerMethodId, MethodCallExpr expr, IndexedSymbols out) {
        SourceRange range = rangeOf(expr);
        String relPosix = posixKey(sourceFile.relativePath());
        try {
            ResolvedMethodDeclaration resolved = expr.resolve().asMethod();
            String declType = resolved.declaringType().getQualifiedName();
            StringBuilder pt = new StringBuilder();
            for (int i = 0; i < resolved.getNumberOfParams(); i++) {
                if (i > 0) {
                    pt.append(',');
                }
                pt.append(resolved.getParam(i).describeType());
            }
            String ret = resolved.getReturnType().describe();
            String calleePrimary =
                    SymbolIdentity.primary(declType, resolved.getName(), pt.toString(), ret);
            String calleeId = "meth:" + calleePrimary;

            ensureNode(
                    out,
                    new GraphNode(
                            calleeId,
                            NodeKind.METHOD,
                            calleePrimary,
                            null,
                            null,
                            range,
                            "{}",
                            snapshotId));

            out.addEdgeCandidate(
                    EdgeCandidate.builder(callerMethodId, calleeId, EdgeKind.CALLS, relPosix)
                            .evidenceSource(EvidenceSource.SOLVER)
                            .confidence(Confidence.HIGH)
                            .sourceRange(range)
                            .reason("resolved_method_call")
                            .build());

        } catch (RuntimeException ex) {
            log.debug("Unresolved call in {}: {} ({})", relPosix, expr, ex.toString());
            String fallbackKey = SymbolIdentity.fallback(
                    repoRoot.toString(), relPosix, range, "call:" + expr.getNameAsString());
            String calleeId = "methfb:" + fallbackKey;

            ensureNode(
                    out,
                    new GraphNode(
                            calleeId,
                            NodeKind.METHOD,
                            null,
                            fallbackKey,
                            relPosix,
                            range,
                            "{\"unresolved\":true,\"name\":\"" + escapeJson(expr.getNameAsString()) + "\"}",
                            snapshotId));

            out.addEdgeCandidate(
                    EdgeCandidate.builder(callerMethodId, calleeId, EdgeKind.CALLS, relPosix)
                            .evidenceSource(EvidenceSource.NAME_MATCH)
                            .confidence(Confidence.LOW)
                            .sourceRange(range)
                            .reason("unresolved_method_call")
                            .build());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static JsonObject annotationToJson(AnnotationExpr ann) {
        JsonObject o = new JsonObject();
        o.addProperty("simple", ann.getNameAsString());
        if (ann.isNormalAnnotationExpr()) {
            JsonObject pairs = new JsonObject();
            ann.asNormalAnnotationExpr()
                    .getPairs()
                    .forEach(p -> pairs.add(p.getNameAsString(), expressionToJson(p.getValue())));
            o.add("pairs", pairs);
        } else if (ann.isSingleMemberAnnotationExpr()) {
            o.add("value", expressionToJson(ann.asSingleMemberAnnotationExpr().getMemberValue()));
        } else if (ann.isMarkerAnnotationExpr()) {
            o.add("marker", new JsonPrimitive(true));
        }
        return o;
    }

    private static JsonElement expressionToJson(Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return new JsonPrimitive(expr.asStringLiteralExpr().asString());
        }
        if (expr.isArrayInitializerExpr()) {
            JsonArray arr = new JsonArray();
            expr.asArrayInitializerExpr().getValues().forEach(v -> arr.add(expressionToJson(v)));
            return arr;
        }
        return new JsonPrimitive(expr.toString());
    }

    private void ensureNode(IndexedSymbols out, GraphNode node) {
        if (declaredNodeIds.add(node.id())) {
            out.addNode(node);
        }
    }

    private static String guessFqName(CompilationUnit cu, String simple) {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString() + ".").orElse("");
        return pkg + simple;
    }

    private static SourceRange rangeOf(com.github.javaparser.ast.Node node) {
        return node.getRange()
                .map(r -> new SourceRange(
                        r.begin.line, r.begin.column, r.end.line, r.end.column))
                .orElseGet(EdgeCandidate::openapiSyntheticRange);
    }

    private static String posixKey(Path repoRelative) {
        StringBuilder sb = new StringBuilder();
        for (Path part : repoRelative) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(part.toString());
        }
        return sb.toString();
    }
}
