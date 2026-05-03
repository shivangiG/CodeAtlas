package com.sgarsgaya.codeatlas.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgarsgaya.codeatlas.api.CapabilitiesApi;
import com.sgarsgaya.codeatlas.constants.AppConstants;
import com.sgarsgaya.codeatlas.model.ListServiceClients200Response;
import com.sgarsgaya.codeatlas.model.SearchCapabilities200Response;
import com.sgarsgaya.codeatlas.model.SearchCapabilitiesRequest;

// TODO: implement — returns 501 until the capability subsystem is in place
@RestController
@RequestMapping(AppConstants.API_BASE)
public class CapabilitiesController implements CapabilitiesApi {

    @Override
    public ResponseEntity<SearchCapabilities200Response> searchCapabilities(
            SearchCapabilitiesRequest searchCapabilitiesRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<ListServiceClients200Response> listServiceClients() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
}
