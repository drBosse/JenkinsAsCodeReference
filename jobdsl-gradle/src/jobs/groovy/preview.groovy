import job.JobBuilder

import javaposse.jobdsl.dsl.DslFactory

new JobBuilder(this as DslFactory, "jenkins_code_review-pipeline", "pipeline")
    .addLogRotator()
    .addScmPollTrigger("H/3 * * * *")
    .addPipelineDefinitionFile("jobdsl-gradle/src/jobs/resources/pipelines/jenkinsreview.groovy")
    .build()
