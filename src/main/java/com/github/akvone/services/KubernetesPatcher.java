package com.github.akvone.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.akvone.properties.OpenShiftProperties;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import lombok.SneakyThrows;

import java.io.IOException;

public class KubernetesPatcher {
    private final OpenShiftProperties props;

    public KubernetesPatcher(OpenShiftProperties properties) {
        this.props = properties;
    }


    public void patchDeployment(String newImagePath) throws IOException, ApiException {
        System.out.println("props.configPath: " + props.configPath);
        AppsV1Api api = new AppsV1Api(Config.fromConfig("c:\\Users\\yust0313\\.kube\\config"));


        // json-patch a deployment
        V1Deployment deploy =
                PatchUtils.patch(
                        V1Deployment.class,
                        () ->
                                api.patchNamespacedDeploymentCall(
                                        props.deployment,
                                        props.namespace,
                                        new V1Patch(prepareJson(newImagePath)),
                                        null,
                                        null,
                                        null, // field-manager is optional
                                        null,
                                        null),
                        V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                        api.getApiClient());
        System.out.println("json-patched deployment" + deploy);
    }

    @SneakyThrows
    private String prepareJson(String newImagePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object object =
                ImmutableMap.of(
                        "spec", ImmutableMap.of(
                                "template", ImmutableMap.of(
                                        "spec", ImmutableMap.of(
                                                "containers", ImmutableList.of(
                                                        ImmutableMap.of(
                                                                "name", props.deployment,
                                                                "image", newImagePath

                                                        )
                                                )
                                        )
                                )
                        )
                );
        String patch = objectMapper.writeValueAsString(object);
        return patch;
    }
}
