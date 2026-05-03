package com.sgarsgaya.codeatlas.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.CiApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.model.CreateBaseline201Response;
import com.sgarsgaya.codeatlas.model.CreateGateEvaluation200Response;
import com.sgarsgaya.codeatlas.model.CreateGateEvaluationRequest;

// TODO: implement — returns 501 until the CI gate evaluator is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class CiController implements CiApi {

    @Override
    public ResponseEntity<CreateGateEvaluation200Response> createGateEvaluation(
            CreateGateEvaluationRequest createGateEvaluationRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<CreateBaseline201Response> createBaseline() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
}
