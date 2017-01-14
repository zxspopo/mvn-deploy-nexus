import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by zengxs on 2016/7/21.
 */
public class MvnDeploy {

    private static final String mvn_deploy =
            "cmd /k mvn deploy:deploy-file -Dfile={0} -DpomFile={1} -DgroupId={2} -DartifactId={3} -Dversion={4} -Dpackaging={5} -DrepositoryId={6} -Durl={7}";



    public static void main(String[] args) throws IOException, InterruptedException {
        Properties properties = new Properties();
        try (InputStream in = MvnDeploy.class.getClassLoader().getResourceAsStream("deploy.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("jar.base.path is " + properties.getProperty("jar.base.path"));
        System.out.println("nexus repository url is " + properties.getProperty("nexus.path"));
        String repoId = properties.getProperty("nexus.repo.id");
        String localRepoDirPath = properties.getProperty("jar.local.dir.path");
        List<File> fileList = filterFile(properties.getProperty("jar.base.path"));
        for (File file : fileList) {
            String filePath = file.getAbsolutePath();
            String pomPath = filePath.substring(0, filePath.lastIndexOf(".")) + ".pom";
            String mvnStr = filePath.substring(localRepoDirPath.length());
            if (mvnStr.startsWith(File.separator)) {
                mvnStr = mvnStr.substring(1);
            }
            String[] mvnStrArray = mvnStr.split("\\" + File.separator);
            int len = mvnStrArray.length;
            String jarName = mvnStrArray[len - 1];
            String version = mvnStrArray[len - 2];
            String artid = mvnStrArray[len - 3];
            StringBuilder groupid = new StringBuilder();
            for (int i = 0; i < len - 3; i++) {
                groupid.append(mvnStrArray[i] + ".");
            }
            groupid.deleteCharAt(groupid.length() - 1);

            String jarDeploy =
                    MessageFormat.format(mvn_deploy, filePath,pomPath, groupid, artid, version, "jar", repoId,
                            properties.getProperty("nexus.path"));
            System.out.println("exec :" + jarDeploy);
            upload(jarDeploy);
        }

    }

    private static void upload(String jarDeploy) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(jarDeploy);
        Thread thread = new Thread() {
            public void run() {
                try (InputStreamReader isr = new InputStreamReader(process.getInputStream())) {
                    BufferedReader br = new BufferedReader(isr);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("Final Memory:")) {
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        thread.join();
    }

    private static List<File> filterFile(String basePath) {
        File baseFile = new File(basePath);
        if (!baseFile.exists()) {
            System.out.println(basePath + "is not exists!");
            System.exit(1);
        }
        List<File> fileList = new ArrayList<>();
        collectJars(baseFile, fileList);
        return fileList;
    }

    private static void collectJars(File baseFile, final List<File> fileList) {
        File[] files = baseFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    collectJars(pathname, fileList);
                } else if (pathname.getName().endsWith("RC001.jar")) {
                    return true;
                }
                return false;
            }
        });
        fileList.addAll(Arrays.asList(files));

    }
}
