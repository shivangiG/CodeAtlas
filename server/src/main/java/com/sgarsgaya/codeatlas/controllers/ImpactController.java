package com.sgarsgaya.codeatlas.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.ImpactsApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.model.CreateImpactAnalysis200Response;
import com.sgarsgaya.codeatlas.model.CreateImpactAnalysisRequest;

// TODO: implement — returns 501 until the impact analyser is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class ImpactController implements ImpactsApi {

    @Override
    public ResponseEntity<CreateImpactAnalysis200Response> createImpactAnalysis(
            CreateImpactAnalysisRequest createImpactAnalysisRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
}
