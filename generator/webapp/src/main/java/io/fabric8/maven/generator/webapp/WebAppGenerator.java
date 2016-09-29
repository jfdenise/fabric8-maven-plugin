/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.generator.webapp;

import io.fabric8.maven.core.util.Configs;
import io.fabric8.maven.core.util.MavenUtil;
import io.fabric8.maven.docker.config.AssemblyConfiguration;
import io.fabric8.maven.docker.config.BuildImageConfiguration;
import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.generator.api.FromSelector;
import io.fabric8.maven.generator.api.MavenGeneratorContext;
import io.fabric8.maven.generator.api.support.BaseGenerator;
import io.fabric8.utils.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kameshs
 */
public class WebAppGenerator extends BaseGenerator {

    private static final String VANNILA_DOCKER_FROM = "fabric8/tomcat-8";
    //TODO need to update this
    private static final String VANNILA_S2I_FROM = "fabric8/tomcat-8";
    //TODO need to update this
    private static final String REDHAT_DOCKER_FROM = "jboss/wildfly";
    private static final String REDHAT_DOCKER_S2I = "jboss-eap-6/eap64-openshift";

    public WebAppGenerator(MavenGeneratorContext context) {
        super(context, "webapp",new FromSelector.Default(context,
                VANNILA_DOCKER_FROM,
                VANNILA_S2I_FROM,
                REDHAT_DOCKER_FROM,
                REDHAT_DOCKER_S2I));
    }

    private enum Config implements Configs.Key {
        deploymentDir  {{ d = "/deployments"; }},
        user           {{ d = "jboss:jboss:jboss"; }},
        cmd            {{ d = "/opt/tomcat/bin/deploy-and-run.sh"; }},
        webPort        {{ d = "8080"; }},
        jolokiaPort    {{ d = "8778"; }};

        public String def() { return d; } protected String d;
    }


    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        boolean isApplicable =  shouldAddDefaultImage(configs) &&
                MavenUtil.hasPlugin(getProject(), "org.apache.maven.plugins:maven-war-plugin");
        log.debug("Is the generator attached ...",isApplicable);
        return isApplicable;
    }

    protected Map<String, String> getEnv() {
        Map<String, String> defaultEnv = new HashMap<>();
        defaultEnv.put("DEPLOY_DIR", getConfig(Config.deploymentDir));
        return defaultEnv;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs) {
        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();

        BuildImageConfiguration.Builder buildBuilder = new BuildImageConfiguration.Builder()
                .assembly(createAssembly())
                .from(getFrom())
                .ports(extractPorts())
                .cmd(getConfig(Config.cmd))
                .env(getEnv());
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
                .name(getImageName())
                .alias(getAlias())
                .buildConfig(buildBuilder.build());
        configs.add(imageBuilder.build());

        return configs;
    }

    protected List<String> extractPorts() {
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.webPort));
        addPortIfValid(answer, getConfig(Config.jolokiaPort));
        return answer;
    }

    private void addPortIfValid(List<String> list, String port) {
        if (Strings.isNotBlank(port)) {
            list.add(port);
        }
    }

    private AssemblyConfiguration createAssembly() {
        return new AssemblyConfiguration.Builder()
                .basedir(getConfig(Config.deploymentDir))
                .user(getConfig(Config.user))
                .descriptorRef("webapp")
                .build();
    }

}
