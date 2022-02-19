package com.github.akvone.services;

import com.github.akvone.properties.OpenShiftProperties;
import io.kubernetes.client.openapi.ApiException;

import java.io.IOException;



public class KubernetesPatcherT {
    private  OpenShiftProperties props = new OpenShiftProperties();
    private  KubernetesPatcher patcher = new KubernetesPatcher(props);
    public KubernetesPatcherT() {
        props.namespace = "rtcd-dev03";
        props.appName = "rtcd-engine-service";
    }


    public void name() throws IOException, ApiException {
        //patcher.patchDeployment("artifactorycn.netcracker.com:17009/nc.sandbox.docker.dev/rtcd-engine-service-parent:20220218191271");
    }
}