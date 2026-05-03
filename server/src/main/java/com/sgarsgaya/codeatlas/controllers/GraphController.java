package com.sgarsgaya.codeatlas.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.GraphApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.model.CreateTaskContext200Response;
import com.sgarsgaya.codeatlas.model.CreateTaskContextRequest;
import com.sgarsgaya.codeatlas.model.GetCallFlow200Response;
import com.sgarsgaya.codeatlas.model.GetSummary200Response;
import com.sgarsgaya.codeatlas.model.GetSymbol200Response;
import com.sgarsgaya.codeatlas.model.ListSymbols200Response;

// TODO: implement — returns 501 until the indexing pipeline is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class GraphController implements GraphApi {

    @Override
    public ResponseEntity<GetSummary200Response> getSummary() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<ListSymbols200Response> listSymbols(String query, String kind, String snapshotId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<GetSymbol200Response> getSymbol(
            String symbolId, Boolean includeSource, Integer maxLinesPerSymbol) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<GetCallFlow200Response> getCallFlow(String from, Integer maxDepth) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<CreateTaskContext200Response> createTaskContext(
            CreateTaskContextRequest createTaskContextRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
}
