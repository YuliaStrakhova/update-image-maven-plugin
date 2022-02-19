package com.github.akvone.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.akvone.properties.OpenShiftProperties;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public class KubernetesPatcher {
    private final OpenShiftProperties props;

    public KubernetesPatcher(OpenShiftProperties properties) {
        this.props = properties;
    }


    public void patchDeployment(String newImagePath) throws IOException, ApiException, InterruptedException {
        System.out.println("props.configPath: " + props.configPath);
        ApiClient apiClient = Config.fromConfig(props.configPath);
        AppsV1Api api = new AppsV1Api(apiClient);


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


        CoreV1Api coreApi = new CoreV1Api(apiClient);

        String fs = "metadata.labels.name=" + props.deployment;
        String ls = "name=" + props.deployment;


        Thread.sleep(5000);

        for (int i = 0; i < 30; i++) {
            Thread.sleep(7000);
            V1PodList list =
                    coreApi.listNamespacedPod(props.namespace, null, null, null, null, ls, null, null, null, null);

            log.info("\n");
            list.getItems().stream().forEach(
                    item -> {
                        log.info(
                                item.getSpec().getContainers().stream().map(
                                                c -> "    " + " " + c.getImage())
                                        .collect(Collectors.joining(","))
                                        +
                                        "  pod.ready=" +
                                        item.getStatus().getContainerStatuses().stream().map(
                                                s -> s.getReady().toString()
                                        ).collect(Collectors.joining(","))
                        );
                    }
            );

            if (list.getItems().stream()
                    .map(V1Pod::getStatus)
                    .map(V1PodStatus::getContainerStatuses)
                    .flatMap(Collection::stream)
                    .allMatch(V1ContainerStatus::getReady)
            ) {
                return;
            }

        }
        throw new RuntimeException("Some problem with deployment");
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
