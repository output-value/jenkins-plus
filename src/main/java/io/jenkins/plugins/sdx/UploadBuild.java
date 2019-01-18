package io.jenkins.plugins.sdx;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by dx on 19-1-18.
 */
public class UploadBuild extends Recorder {


    public final String upload;
    public final String submit;
    public final String params;


    @DataBoundConstructor
    public UploadBuild(String upload, String submit, String params) {
        this.upload = upload;
        this.submit = submit;
        this.params = params;
    }

    //和前台配置相关
    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            return false;
        }
        if (params == null || !params.contains("$")) {
            listener.getLogger().println("不能不获取参数啊");
            return false;
        }
        EnvVars environment = build.getEnvironment(listener);


        List<String> fileName = findFile(build.getWorkspace());
        //构建类型..当然可以修改为自己的参数

        String serviceType = environment.get("SERVICE_TYPE", "0");

        for (String path : fileName) {
            UploadInfo info = UploadClient.postFile(upload, new File(path));
            listener.getLogger().println(info);
        }

        return true;
    }


    private List<String> findFile(FilePath directory) throws IOException, InterruptedException {

        List<FilePath> files = directory.list();
        if (files == null) {
            files = new ArrayList<>();
        }
        List<String> tempList = new ArrayList<>();
        for (FilePath file : files) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".apk")) {
                    tempList.add(file.getRemote());
                }
            } else {
                tempList.addAll(findFile(file));
            }
        }

        return tempList;
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(String upload) {
            load();
        }

        public DescriptorImpl(String upload, String submit) {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return true;
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "upload";
        }

    }
}
