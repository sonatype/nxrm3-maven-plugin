library('private-pipeline-library')
library('jenkins-shared')

mavenSnapshotPipeline(performSonarAnalysis: true,
    onSuccess: { build, env ->
      notifyChat(env: env, currentBuild: build, room: 'The Build Watchers')
    },
    onFailure: { build, env ->
      notifyChat(env: env, currentBuild: build, room: 'The Build Watchers')
    }
)