// Pipeline doesn't resolve env outside of node so to read node name
// we have to do the trick below
def nodeName = ""
node() { nodeName = env.utility_slave }

node(nodeName) {
  stage('Checkout'){
      //extensions: [[$class: 'CleanBeforeCheckout'],
      // [$class: 'PruneStaleBranch']],
      checkout([$class: 'GitSCM',
        branches: [[name: "*/ready/**"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'CleanBeforeCheckout']],
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: env.default_credentials, url: env.default_repo]]])
  }
  stage('Prepare'){
      sh 'git log -n5 --oneline --graph --decorate'
      sh 'git describe --all > GIT_DESCRIBE'
      git_branch = readFile('GIT_DESCRIBE').replace('remotes/origin/','').trim()
      echo git_branch
      git_remote = readFile('GIT_DESCRIBE').trim().split('/')[1]
      echo git_remote
  }
  stage('CO Integration Branch'){
    sh 'git checkout origin/'+env.default_branch
  }

  stage('Merge the change Branch'){
    echo 'Need to know what ready branch triggered us'
    sh "git merge ${git_remote}/${git_branch}"
  }

  stage('Verify JobDSL'){
    dir('jobdsl-gradle') {
      sh './gradlew buildXml'
      sh './gradlew test'
    }
  }

  stage('build docker compose'){
    dir('dockerizeit') {
      sh 'env && docker-compose build'
    }
  }

  stage('generate compose'){
    dir('dockerizeit'){
      sh './generate-compose.py --debug --file docker-compose.yml --jmaster-image test-image --jmaster-version test-version --jslave-image test-image --jslave-version test-version && cat docker-compose.yml && git checkout HEAD docker-compose.yml'
    }
  }

  stage('munchausen'){
    dir('dockerizeit/munchausen'){
      sh 'cp ../docker-compose.yml .'
      sh 'docker build --build-arg http_proxy --build-arg https_proxy --build-arg no_proxy -t munchausen .'
    }
  }

  stage('merge back'){
    echo git_branch
    sh 'git log -n5 --oneline --decorate --graph'
    // git push origin
  }
}
