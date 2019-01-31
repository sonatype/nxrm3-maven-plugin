/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared']) _
import com.sonatype.jenkins.pipeline.GitHub
import com.sonatype.jenkins.pipeline.OsTools

properties([
    parameters([
        booleanParam(defaultValue: false, description: 'Maven Staging Plugin for NXRM3', name: 'isRelease')
    ])
])

String apiToken = null
withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'integrations-github-api',
                  usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_PASSWORD']]) {
  apiToken = env.GITHUB_API_PASSWORD
}
GitHub gitHub = new GitHub(this, 'sonatype/maven-staging-plugin', apiToken)
Closure postHandler = {
  currentBuild, env ->
    def commitId = OsTools.runSafe(this, 'git rev-parse HEAD')
    if (currentBuild.currentResult == 'SUCCESS') {
      gitHub.statusUpdate commitId, 'success', 'CI', 'CI Passed'
    }
    else {
      gitHub.statusUpdate commitId, 'failure', 'CI', 'CI Failed'
    }
}

Closure iqPolicyEvaluation = {
  stage ->
    def commitId = OsTools.runSafe(this, 'git rev-parse HEAD')
    gitHub.statusUpdate commitId, 'pending', 'analysis', 'Nexus Lifecycle Analysis Running'

    def dependencyDirectory = 'dependencies'
    OsTools.runSafe(this, "mkdir ${dependencyDirectory}")
    withMaven(jdk: 'JDK8u121',
        maven: 'M3',
        mavenSettingsConfig: 'private-settings.xml',
        mavenLocalRepo: "${WORKSPACE}/.zion/repository") {
      OsTools.runSafe(this, "mvn dependency:copy-dependencies -DoutputDirectory=../${dependencyDirectory} -DincludeScope=compile -f maven-plugin/pom.xml")
    }
    OsTools.runSafe(this, "cp maven-plugin/target/nexus-staging-maven-plugin-*.jar ${dependencyDirectory}")

    try {
      def evaluation = nexusPolicyEvaluation iqApplication: 'nexus-staging-maven-plugini',
          iqScanPatterns: [[scanPattern: "${dependencyDirectory}/*.jar"]],
          iqStage: stage
      gitHub.statusUpdate commitId, 'success', 'analysis', 'Nexus Lifecycle Analysis Succeeded', "${evaluation.applicationCompositionReportUrl}"
    }
    catch (error) {
      def evaluation = error.policyEvaluation
      gitHub.statusUpdate commitId, 'failure', 'analysis', 'Nexus Lifecycle Analysis Failed', "${evaluation.applicationCompositionReportUrl}"
      throw error
    }
    finally {
      dir(dependencyDirectory) {
        deleteDir()
      }
    }
}

if (!params.isRelease) {
  mavenSnapshotPipeline(
    notificationSender: postHandler,
    iqPolicyEvaluation: iqPolicyEvaluation
  )
  return
} else {
  node('ubuntu-zion') {
    def commitId, commitDate, pom, version

    try {
      stage('Preparation') {
        deleteDir()

        checkout scm

        commitId = OsTools.runSafe(this, 'git rev-parse HEAD')
        commitDate = OsTools.runSafe(this, "git show -s --format=%cd --date=format:%Y%m%d-%H%M%S ${commitId}")

        OsTools.runSafe(this, 'git config --global user.email sonatype-ci@sonatype.com')
        OsTools.runSafe(this, 'git config --global user.name Sonatype CI')

        pom = readMavenPom file: 'pom.xml'
        version = pom.version.replace("-SNAPSHOT", ".${commitDate}.${commitId.substring(0, 7)}")

        currentBuild.displayName = "#${currentBuild.number} - ${version}"
      }
      stage('License Check') {
        withMaven(jdk: 'JDK8u121',
            maven: 'M3',
            mavenSettingsConfig: 'private-settings.xml',
            mavenLocalRepo: "${WORKSPACE}/.zion/repository") {
          OsTools.runSafe(this, 'mvn license:check')
        }
      }
      stage('Build') {
        withMaven(jdk: 'JDK8u121',
            maven: 'M3',
            mavenSettingsConfig: 'private-settings.xml',
            mavenLocalRepo: "${WORKSPACE}/.zion/repository") {
          OsTools.runSafe(this, 'mvn clean install')
        }
      }
      stage('Nexus Lifecycle Analysis') {
        iqPolicyEvaluation('release')
      }
      stage('Archive Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
        archive 'nexus-staging-maven-plugin/target/proguard_*.txt'
      }
    }
    finally {
      postHandler(env, currentBuild)
    }
    
    if (currentBuild.result == 'FAILURE') {
      return
    }
    if (scm.branches[0].name != '*/master') {
      return
    }
    stage('Deploy to Sonatype') {
      withGpgCredentials('gnupg') {
        withMaven(jdk: 'JDK8u121',
            maven: 'M3',
            mavenSettingsConfig: 'private-settings.xml',
            mavenLocalRepo: "${WORKSPACE}/.zion/repository") {
          OsTools.runSafe(this, "mvn -Darguments=-DskipTests -DreleaseVersion=${version} -DdevelopmentVersion=${pom.version} -DpushChanges=false -DlocalCheckout=true -DpreparationGoals=initialize release:prepare release:perform -B")
        }
      }
    }
    input 'Push tags?'
    stage('Push tags') {
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'integrations-github-api',
                        usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_PASSWORD']]) {
        OsTools.runSafe(this,
            "git push https://${env.GITHUB_API_USERNAME}:${env.GITHUB_API_PASSWORD}@github.com/sonatype/maven-staging-plugin.git ${pom.artifactId}-${version}")
      }

      // Reset all changes to local repository made by the Maven release plugin
      OsTools.runSafe(this, "git tag -d ${pom.artifactId}-${version}")
      OsTools.runSafe(this, 'git clean -f && git reset --hard origin/master')
    }
  }
}
