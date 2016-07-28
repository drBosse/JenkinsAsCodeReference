// Pipeline doesn't resolve env outside of node so to read node name
// we have to do the trick below
def nodeName = ""
node() { nodeName = env.utility_slave }

node(nodeName) {
   stage 'Checkout'
   checkout([$class: 'GitSCM',
             branches: [[name: "*/ready/**"]],    // */${env.default_branch}
             doGenerateSubmoduleConfigurations: false,
             extensions: [[$class: 'CleanBeforeCheckout']],
             submoduleCfg: [],
             userRemoteConfigs: [[credentialsId: env.default_credentials, url: env.default_repo]]])

   stage 'Merge Integration Branch'
   sh 'git merge origin/'+env.default_branch

   stage 'Verify JobDSL'
   dir('jobdsl-gradle') {
     sh './gradlew buildXml'
     sh './gradlew test'
   }

   stage 'build docker compose'
   dir('dockerizeit') {
      sh 'env && docker-compose build'
   }

  stage 'generate compose'
  dir('dockerizeit'){
      sh './generate-compose.py --debug --file docker-compose.yml --jmaster-image test-image --jmaster-version test-version --jslave-image test-image --jslave-version test-version && cat docker-compose.yml && git checkout HEAD docker-compose.yml'
  }

  stage 'munchausen'
  dir('dockerizeit/munchausen'){
    sh 'cp ../docker-compose.yml .'
    sh 'docker build --build-arg http_proxy --build-arg https_proxy --build-arg no_proxy -t munchausen .'
  }
  stage 'merge back'
  sh 'git log --oneline --graph --decorate'
  sh 'git describe --all | sed \'s|remotes/||\' > branch.txt'
  def git_branch=readFile('branch.txt').trim()
  echo git_branch
  if(git_branch != 'origin'+env.default_branch) {
    sh 'git checkout origin/'+env.default_branch
    sh 'git merge '+git_branch
    // git push origin
  }
  sh 'git log --oneline --graph --decorate'
}
