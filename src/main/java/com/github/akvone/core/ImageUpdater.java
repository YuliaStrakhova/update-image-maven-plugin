package com.github.akvone.core;

import com.github.akvone.properties.OpenShiftProperties;
import com.github.akvone.properties.PropertiesHolder;
import com.github.akvone.services.DockerBuilder;
import com.github.akvone.services.KubernetesPatcher;
import com.github.akvone.services.OpenShiftPatcher;
import com.github.akvone.services.YamlPropsService;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

@Slf4j
public class ImageUpdater {

    private final MavenProject project;
    private final String projectArtifactId;
    private final boolean alsoMakeUpdateInCloud;

    private Runnable preExecuteCallback = () -> {
    };

    public ImageUpdater(MavenProject project, boolean alsoMakeUpdateInCloud) {
        this.project = project;
        this.projectArtifactId = project.getArtifactId();
        this.alsoMakeUpdateInCloud = alsoMakeUpdateInCloud;
    }

    public void setPreExecuteCallback(Runnable callback) {
        this.preExecuteCallback = callback;
    }

    public void execute() {
        preExecuteCallback.run();
        PropertiesHolder propsHolder = new YamlPropsService().createPropertiesHolder();

        String imageLocation = uploadDockerImage(propsHolder);

        if (alsoMakeUpdateInCloud) {
            patchDeployment(propsHolder, imageLocation);
        }
    }

    private void patchDeployment(PropertiesHolder propsHolder, String imageLocation) {
        if (propsHolder.get("cloudProvider", "type").equals("kubernetes")) {
            log.info("Start to patch Kubernetes deployment with an image: {}", imageLocation);
            KubernetesPatcher kubernetesPatcher = new KubernetesPatcher(generateOpenShiftProperties(propsHolder));
            try {
                kubernetesPatcher.patchDeployment(imageLocation);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ApiException e) {
                e.printStackTrace();
            }
        } else {
            log.info("Start to patch OpenShift deployment with an image: {}", imageLocation);
            OpenShiftPatcher openShiftPatcher = new OpenShiftPatcher(generateOpenShiftProperties(propsHolder));
            openShiftPatcher.patchOpenShiftDeployment(imageLocation);
        }
    }

    private String uploadDockerImage(PropertiesHolder propsHolder) {
        log.info("Starting to connect to docker");
        DockerBuilder dockerBuilder = new DockerBuilder(propsHolder, projectArtifactId);
        log.info("Connected to docker");

        String imagePath = null;
        try {
            log.info("Starting building and pushing image...");
            imagePath = dockerBuilder.run();
            log.info("Push is successful. Image location: {}", imagePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return imagePath;
    }

    private OpenShiftProperties generateOpenShiftProperties(PropertiesHolder propsHolder) {
        OpenShiftProperties props = new OpenShiftProperties();
        props.serverUrl = propsHolder.get("cloudProvider", "url");
        props.namespace = propsHolder.get("cloudProvider", "namespace");
        props.authorization = propsHolder.get("cloudProvider", "authorizationToken");
        props.configPath = propsHolder.get("cloudProvider", "configPath");
        props.deployment = propsHolder.get("cloudProvider", "deployment");

        props.appName = projectArtifactId;

        return props;
    }

}
