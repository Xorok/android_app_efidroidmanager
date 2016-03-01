package org.efidroid.efidroidmanager;

import android.util.Log;

import com.stericson.rootshell.RootShell;
import com.stericson.rootshell.exceptions.RootDeniedException;
import com.stericson.rootshell.execution.Command;
import com.stericson.rootshell.execution.Shell;
import com.stericson.roottools.RootTools;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.services.IntentServiceEx;
import org.efidroid.efidroidmanager.types.MountEntry;
import org.efidroid.efidroidmanager.types.Pointer;
import org.efidroid.efidroidmanager.types.ReturnCodeException;

public final class RootToolsEx {
    public interface MountInfoLoadedCallback {
        void onError(Exception e);
        void onSuccess(List<MountEntry> mountEntry);
    }

    public static void commandWait(Shell shell, Command cmd) throws Exception {
        while (!cmd.isFinished()) {

            RootShell.log(RootShell.version, shell.getCommandQueuePositionString(cmd));
            RootShell.log(RootShell.version, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.");

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
    }

    public static void commandWaitService(Shell shell, Command cmd, int pid, IntentServiceEx service) throws Exception {
        while (!cmd.isFinished()) {
            if(service.shouldStop()) {
                shell.close();
                ReturnCodeException.check(kill(pid));
                throw new Exception("command was killed");
            }

            RootShell.log(RootShell.version, shell.getCommandQueuePositionString(cmd));
            RootShell.log(RootShell.version, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.");

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * @return <code>true</code> if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    public static boolean isAccessGiven(int timeout, int retry) {
        final Set<String> ID = new HashSet<String>();
        final int IAG = 158;

        try {
            RootShell.log("Checking for Root access");

            Command command = new Command(IAG, false, "busybox id") {
                @Override
                public void commandOutput(int id, String line) {
                    if (id == IAG) {
                        ID.addAll(Arrays.asList(line.split(" ")));
                    }

                    super.commandOutput(id, line);
                }
            };

            Shell.startRootShell(timeout, retry).add(command);
            commandWait(Shell.startRootShell(timeout, retry), command);
            ReturnCodeException.check(command.getExitCode());

            //parse the userid
            for (String userid : ID) {
                RootShell.log(userid);

                if (userid.toLowerCase().contains("uid=0")) {
                    RootShell.log("Access Given");
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static MountInfo getMountInfo() throws Exception {
        final ArrayList<MountEntry> mountinfos = new ArrayList<>();

        final Command command = new Command(0, false, "busybox cat /proc/1/mountinfo")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);

                String[] fields = line.split(" ");
                String[] majmin = fields[2].split(":");
                mountinfos.add(new MountEntry(
                        Integer.parseInt(fields[0]), // mountID
                        Integer.parseInt(fields[1]), // parentID
                        Integer.parseInt(majmin[0]), // major
                        Integer.parseInt(majmin[1]), // minor
                        fields[3], // root
                        fields[4], // mountPoint
                        fields[5], // mountOptions
                        fields[6], // optionalFields
                        fields[7], // separator
                        fields[8], // fsType
                        fields[9], // mountSource
                        fields[10] // superOptions
                ));
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return new MountInfo(mountinfos);
    }

    public static List<String> getBlockDevices() throws Exception {
        final ArrayList<String> devices = new ArrayList<>();

        final Command command = new Command(0, false, "busybox blkid")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String blkDevice = line.split(":")[0];
                devices.add(blkDevice);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return devices;
    }

    public static int[] getDeviceNode(String path) throws Exception {
        final Pointer<Integer> major = new Pointer<>(0);
        final Pointer<Integer> minor = new Pointer<>(0);

        final Command command = new Command(0, false, "busybox stat -Lt \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String[] parts = line.split(" ");

                major.value = Integer.parseInt(parts[9], 16);
                minor.value = Integer.parseInt(parts[10], 16);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return new int[]{major.value, minor.value};
    }

    public static boolean fileExists(String path) throws Exception {
        Command command = new Command(0, false, "busybox ls \""+path+"\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode()==0;
    }

    public static List<String> getMultibootSystems(String path) throws Exception {
        final ArrayList<String> directories = new ArrayList<>();

        final Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 1 -maxdepth 1")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                directories.add(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return directories;
    }

    public static boolean isDirectory(String path) throws Exception {
        final Pointer<Boolean> isDir = new Pointer<>(false);

        final Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 0 -maxdepth 0 -type d")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                isDir.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return isDir.value;
    }

    public static long getDeviceSize(String path) throws Exception {
        final Pointer<Long> size = new Pointer<>(new Long(-1));

        final Command command = new Command(0, false, "busybox blockdev --getsize64 \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String[] parts = line.split(" ");

                size.value = Long.parseLong(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return size.value;
    }

    public static String readFile(String path) throws Exception {
        final StringWriter stringWriter = new StringWriter();

        final Command command = new Command(0, false, "busybox cat \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                stringWriter.write(line);
                stringWriter.write("\n");
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        if(command.getExitCode()!=0)
            return null;

        return stringWriter.getBuffer().toString();
    }

    public static int kill(int pid) throws Exception {
        final Command command = new Command(0, false, "busybox kill -SIGKILL "+pid);
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode();
    }

    public static int runServiceCommand(IntentServiceEx service, final Command command) throws Exception {
        final Pointer<Integer> pid = new Pointer<>(0);

        final Command pidcommand = new Command(0, false, 0, "busybox dd if=/dev/zero of=/dev/null &\n echo $!")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                pid.value = Integer.parseInt(line);
            }
        };

        // run command async
        Shell shell = RootTools.getShell(true);
        shell.add(pidcommand);
        commandWait(shell, pidcommand);
        ReturnCodeException.check(pidcommand.getExitCode());

        // wait for async command to finish
        final Command waitcommand = new Command(0, false, 0, "wait "+pid.value);
        shell = RootTools.getShell(true);
        shell.add(waitcommand);
        commandWaitService(shell, waitcommand, pid.value, service);
        return waitcommand.getExitCode();
    }

    public static void die(IntentServiceEx service) throws Exception {
        final Command command = new Command(0, false, 0, "busybox dd if=/dev/zero of=/dev/null");
        runServiceCommand(service, command);
    }

    public static class RootFile extends File {
        private boolean mIsDir;

        public RootFile(String path, boolean isDir) {
            super(path);
            mIsDir = isDir;
        }

        public RootFile(String path) {
            super(path);
            try {
                mIsDir = RootToolsEx.isDirectory(path);
            } catch (Exception e) {
                mIsDir = false;
            }
        }

        @Override
        public boolean isDirectory() {
            return mIsDir;
        }

        @Override
        public boolean isFile() {
            return !mIsDir;
        }

        @Override
        public File getParentFile() {
            return new RootFile(super.getParent());
        }

        @Override
        public File[] listFiles() {
            final String path = getAbsolutePath();
            final ArrayList<RootFile> list = new ArrayList<>();
            final String prefix = path.charAt(path.length()-1)=='/'?path:path+"/";

            final Command command = new Command(0, false, "busybox ls -lL \""+path+"\"")
            {
                @Override
                public void commandOutput(int id, String line) {
                    super.commandOutput(id, line);
                    String[] parts = line.split(" ");

                    boolean is_dir = parts[0].charAt(0)=='d';
                    String name = parts[parts.length-1];
                    list.add(new RootFile(prefix+name, is_dir));
                }
            };

            try {
                Shell shell = RootTools.getShell(true);
                shell.add(command);
                commandWait(shell, command);
                if(command.getExitCode()!=0)
                    list.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list.toArray(new RootFile[]{});
        }
    }
}
