#!/usr/bin/env -S scala-cli shebang --quiet

//> using scala 3.6.3
//> using jvm 21
//> using toolkit 0.7.0
//> using dep org.encalmo::script-utils:0.9.0

import java.nio.file.Path
import scala.io.AnsiColor.*
import org.encalmo.utils.CommandLineUtils.*
import sttp.client4.*
import sttp.model.Uri

val version = requiredScriptParameter("version")(args)
val secretKeyPassword = optionalScriptParameter("secret-key-password")(args)
val maybeGpgKey = optionalScriptParameter("gpg-key")(args)
val maybeSecretKey = optionalScriptParameter("secret-key")(args)
val publishScalaJs = optionalScriptFlag("js")(args)
val publishScalaNative = optionalScriptFlag("native")(args)
val uploadToSonatypeCentral = optionalScriptFlag("upload-to-sonatype-central")(args)
val sonatypeToken = optionalScriptParameter("sonatype-token")(args)

val signer = maybeGpgKey match {
  case Some(key) => s"""--signer gpg --gpg-key $key"""
  case None =>
    maybeSecretKey match {
      case Some(key) =>
        s"""--signer bc --secret-key $key${secretKeyPassword
            .map(value => s" --secret-key-password value:$value")
            .getOrElse("")}"""
      case None => ""
    }

}

val usingDirectiveRegex = """\/\/\>\s+using\s+(.+)\s+(.+)""".r

val (name, organization) = {
  val config =
    os
      .read(os.pwd / "project.scala")
      .linesIterator
      .map {
        case usingDirectiveRegex(name, value) => Some((name, value))
        case _                                => None
      }
      .collect { case Some(x) => x }
      .toMap
  {
    for {
      name <- config.get("publish.name").map(_.stripPrefix("\"").stripSuffix("\""))
      organization <- config.get("publish.organization").map(_.stripPrefix("\"").stripSuffix("\""))
    } yield (name, organization)
  }.getOrElse(
    throw new Exception("File project.scala should contain publish.name and publish.organization directives")
  )
}

println(s"${GREEN}Found config publish.name=$name publish.organization=$organization${RESET}")

println(s"${GREEN}Running tests ...${RESET}")

call(
  s"scala-cli --power test . --suppress-deprecated-warnings --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-deprecated-feature-warning"
).foreach(
  println
)

val scalaJvmCommand =
  s"""scala-cli --power publish local . --organization $organization --name $name --project-version $version $signer --suppress-deprecated-warnings --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-deprecated-feature-warning"""

publishUsing(scalaJvmCommand)

if (publishScalaJs) then {
  val scalaJsCommand =
    s"""scala-cli --power publish local --js . --organization $organization --name $name --project-version $version $signer --suppress-deprecated-warnings --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-deprecated-feature-warning"""

  publishUsing(scalaJsCommand)
}

if (publishScalaNative) then {
  val scalaNativeCommand =
    s"""scala-cli --power publish local --native . --organization $organization --name $name --project-version $version $signer --suppress-deprecated-warnings --suppress-experimental-feature-warning --suppress-directives-in-multiple-files-warning --suppress-deprecated-feature-warning"""

  publishUsing(scalaNativeCommand)
}

// --- UTILS ---

def publishUsing(command: String) = {

  println(s"${GREEN}Publishing package locally ...${RESET}")

  val (publishedFolder, coordinates) = {
    val ivyLocation = call(command).last.trim()
    val coordinates = {
      ivyLocation.drop(ivyLocation.indexOf(organization)).split("/")
    }
    (
      if (ivyLocation.startsWith("~" + java.io.File.separator))
      then os.Path(java.io.File(System.getProperty("user.home") + ivyLocation.substring(1)))
      else os.Path(java.io.File(ivyLocation).getAbsoluteFile()),
      coordinates
    )
  }

  val artefactName = coordinates.dropRight(1).last

  println(s"${GREEN}Published ${coordinates.mkString(":")} to $publishedFolder${RESET}")

  val tempDir = os.temp.dir(prefix = s"sonatype-deployment-package-")
  val bundleFolderPath = tempDir / toFolderPath(coordinates)
  os.makeDir.all(bundleFolderPath)

  val bundleFileName = s"${artefactName}-${version}.zip"

  println(s"${GREEN}Preparing sonatype bundle in ${bundleFolderPath} ...${RESET}")

  copyPublishedFiles(publishedFolder, "poms", artefactName, bundleFolderPath)
  copyPublishedFiles(publishedFolder, "jars", artefactName, bundleFolderPath)
  copyPublishedFiles(publishedFolder, "srcs", artefactName, bundleFolderPath)
  copyPublishedFiles(publishedFolder, "docs", artefactName, bundleFolderPath)

  val bundleFilePath = os.pwd / s"$bundleFileName"

  println(s"${GREEN}Creating bundle archive ...${RESET}")
  val bundleArchivePath = tempDir / s"$bundleFileName"
  call(s"zip -r $bundleFileName ${bundleFolderPath.relativeTo(tempDir)}", cwd = tempDir).foreach(println)
  call(s"ls -l $bundleArchivePath").foreach(println)
  call(s"mv $bundleFileName $bundleFilePath", cwd = tempDir).foreach(println)
  println(s"${GREEN}Bundle archive ready at $bundleFilePath${RESET}")

  if (uploadToSonatypeCentral && sonatypeToken.isDefined) then {
    println(s"${GREEN}Uploading ${bundleFileName} to Sonatype Central ...${RESET}")

    val response = basicRequest
      .response(asStringAlways)
      .post(Uri.unsafeParse("https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC"))
      .header("Authorization", s"Bearer ${sonatypeToken.get}")
      .multipartBody(
        multipartFile("bundle", new java.io.File(bundleFilePath.toString())).fileName(bundleFileName)
      )
      .send(quick.backend)
    if (response.code.isSuccess)
    then println(s"${GREEN}Uploaded successfully.${RESET}")
    else {
      println(s"${RED_B}${WHITE}Upload failed with ${response.code.code} ${response.body}${RESET}")
      System.exit(2)
    }
  }

}

def call(command: String, cwd: os.Path = os.pwd): Seq[String] =
  println(s"${BLUE}command: ${command}${RESET}")
  val commandArray = command.split(" ")
  val commandResult = os.proc(commandArray).call(check = false, cwd = cwd)
  if (commandResult.exitCode != 0)
  then {
    println(
      s"${WHITE}${RED_B}[FATAL] script's command ${YELLOW}${commandArray(0)}${WHITE} returned ${commandResult.exitCode} ${RESET}"
    )
    System.exit(2)
    Seq.empty
  } else commandResult.out.lines()

def toFolderPath(coordinates: Array[String]): os.RelPath =
  os.RelPath(coordinates(0).split("\\.") ++ coordinates.drop(1), 0)

def copyPublishedFiles(publishedFolder: os.Path, folder: String, artefactName: String, bundleFolderPath: os.Path) = {
  os.list(publishedFolder / folder)
    .foreach { path =>
      val targetName = path.last.replace(artefactName, s"$artefactName-$version")
      println(s"${GREEN}- copying $targetName ...${RESET}")
      os.copy(from = path, to = bundleFolderPath / targetName)
    }
}
