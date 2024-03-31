# AFS

## Build

```shell
//build
sh gradlew build
//publish
sh gradlew -Pgroup=com.github.storytellerF.AFS clean -xtest -xlint assemble publishToMavenLocal
//也可以选择和jitpack 使用相同的group。
//-Pgroup=com.github.storytellerF.AFS -Pversion=version
```