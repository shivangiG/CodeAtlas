package com.sgarsgaya.codeatlas.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.AnalyzersApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.model.GetReview200Response;
import com.sgarsgaya.codeatlas.model.ListDriftFindings200Response;
import com.sgarsgaya.codeatlas.model.ListViolations200Response;

// TODO: implement — returns 501 until the architecture analyser is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class AnalyzersController implements AnalyzersApi {

    @Override
    public ResponseEntity<ListViolations200Response> listViolations(String confidence) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<ListDriftFindings200Response> listDriftFindings() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<GetReview200Response> getReview(String format) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
}
