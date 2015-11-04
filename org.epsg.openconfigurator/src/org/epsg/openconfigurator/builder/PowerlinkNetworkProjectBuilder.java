/*******************************************************************************
 * @file   PowerlinkNetworkProjectBuilder.java
 *
 * @author Ramakrishnan Periyakaruppan, Kalycito Infotech Private Limited.
 *
 * @copyright (c) 2015, Kalycito Infotech Private Limited
 *                    All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of the copyright holders nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package org.epsg.openconfigurator.builder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.epsg.openconfigurator.Activator;
import org.epsg.openconfigurator.console.OpenConfiguratorMessageConsole;
import org.epsg.openconfigurator.editors.project.IndustrialNetworkProjectEditor;
import org.epsg.openconfigurator.lib.wrapper.ByteCollection;
import org.epsg.openconfigurator.lib.wrapper.OpenConfiguratorCore;
import org.epsg.openconfigurator.lib.wrapper.Result;
import org.epsg.openconfigurator.model.IPowerlinkProjectSupport;
import org.epsg.openconfigurator.model.Path;
import org.epsg.openconfigurator.util.OpenConfiguratorLibraryUtils;

/**
 * Builder implementation for POWERLINK project.
 *
 * @author Ramakrishnan P
 *
 */
public class PowerlinkNetworkProjectBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "org.epsg.openconfigurator.industrialNetworkBuilder";

    /**
     * The list of Industrial network project editors wherein the library has
     * only knowledge about the projects which are open via
     * IndustrialNetworkProjectEditor.
     *
     * @return The list of Industrial network project editors.
     */
    private static List<IndustrialNetworkProjectEditor> getOpenProjectEditors() {
        List<IndustrialNetworkProjectEditor> projectEditors = new ArrayList<IndustrialNetworkProjectEditor>();
        IWorkbenchWindow workbenchWindows[] = PlatformUI.getWorkbench()
                .getWorkbenchWindows();
        for (IWorkbenchWindow window : workbenchWindows) {
            IEditorReference[] editors = window.getActivePage()
                    .getEditorReferences();
            for (IEditorReference editor : editors) {
                if (editor.getEditor(
                        false) instanceof IndustrialNetworkProjectEditor) {
                    IndustrialNetworkProjectEditor pjtEditor = (IndustrialNetworkProjectEditor) editor
                            .getEditor(false);
                    projectEditors.add(pjtEditor);
                }
            }
        }

        return projectEditors;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(final int kind, Map args,
            IProgressMonitor monitor) throws CoreException {
        forgetLastBuiltState();

        switch (kind) {
            case IncrementalProjectBuilder.FULL_BUILD:
            case IncrementalProjectBuilder.CLEAN_BUILD:
            case IncrementalProjectBuilder.INCREMENTAL_BUILD:
                fullBuild(monitor);
                break;
            case IncrementalProjectBuilder.AUTO_BUILD:
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        MessageDialog msd = new MessageDialog(
                                Display.getDefault().getActiveShell(),
                                "Build project", null,
                                "Does not support 'Build Automatically'. Use clean then build project."
                                        + kind,
                                0, new String[] { "Ok" }, 0);
                        msd.open();
                    }
                });
                break;
            default:
                System.err.println("Un supported build type" + kind);
                break;
        }

        return null;
    }

    /**
     * Build the concise device configuration outputs in the specified output
     * path.
     *
     * @param networkId The network ID.
     * @param outputpath The location to save the output files.
     * @param monitor Monitor instance to update the progress activity.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean buildConciseDeviceConfiguration(final String networkId,
            java.nio.file.Path outputpath, final IProgressMonitor monitor)
                    throws CoreException {

        String configurationOutput[] = new String[1];
        ByteCollection cdcByteCollection = new ByteCollection();

        Result res = OpenConfiguratorCore.GetInstance().BuildConfiguration(
                networkId, configurationOutput, cdcByteCollection);
        if (!res.IsSuccessful()) {
            IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    IStatus.OK,
                    OpenConfiguratorLibraryUtils.getErrorMessage(res), null);
            OpenConfiguratorMessageConsole.getInstance().printErrorMessage(
                    OpenConfiguratorLibraryUtils.getErrorMessage(res));
            System.err.println("Build ERR "
                    + OpenConfiguratorLibraryUtils.getErrorMessage(res));
            throw new CoreException(errorStatus);
        } else {

            try {
                if (!Files.exists(outputpath, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectory(outputpath);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                IStatus errorStatus = new Status(IStatus.ERROR,
                        Activator.PLUGIN_ID, IStatus.OK, "Output path:"
                                + outputpath.toString() + " does not exist.",
                        e1);
                throw new CoreException(errorStatus);
            }

            // String[1] is always empty.
            boolean retVal = createMnobdTxt(outputpath, configurationOutput[0]);
            if (!retVal) {
                return retVal;
            }

            ByteBuffer buffer = ByteBuffer
                    .allocate((int) cdcByteCollection.size());

            for (int i = 0; i < cdcByteCollection.size(); i++) {
                short value = cdcByteCollection.get(i);
                // buffer.putShort(value);
                buffer.put((byte) (value & 0xFF));
                // buffer.put((byte) ((value >> 8) & 0xff));
            }

            retVal = createMnobdCdc(outputpath, buffer);
            if (!retVal) {
                return retVal;
            }

            retVal = createMnobdHexTxt(outputpath, buffer);
            if (!retVal) {
                return retVal;
            }
        }

        return true;
    }

    /**
     * Writes the processimage variables for the specified node ID. The format
     * is usable in the 'C' language.
     *
     * @param networkId The network ID.
     * @param nodeId The node for which the processimage to be generated.
     * @param targetPath The location to save the output file.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean buildCProcessImage(String networkId, short nodeId,
            java.nio.file.Path targetPath) throws CoreException {
        String piDataOutput[] = new String[1];
        Result res = OpenConfiguratorCore.GetInstance()
                .BuildCProcessImage(networkId, nodeId, piDataOutput);
        if (!res.IsSuccessful()) {
            IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    IStatus.OK,
                    OpenConfiguratorLibraryUtils.getErrorMessage(res), null);
            OpenConfiguratorMessageConsole.getInstance().printErrorMessage(
                    OpenConfiguratorLibraryUtils.getErrorMessage(res));
            System.err.println("Build ERR "
                    + OpenConfiguratorLibraryUtils.getErrorMessage(res));
            throw new CoreException(errorStatus);
        } else {
            java.nio.file.Path targetFilePath = targetPath
                    .resolve(IPowerlinkProjectSupport.XAP_H);

            try {
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                Files.write(targetFilePath, piDataOutput[0].getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                IStatus errorStatus = new Status(IStatus.ERROR,
                        Activator.PLUGIN_ID, IStatus.OK,
                        "Output file:" + targetFilePath.toString()
                                + " is not accessible.",
                        e);
                throw new CoreException(errorStatus);
            }
        }

        return true;
    }

    /**
     * Writes the processimage variables for the specified node ID. The format
     * is usable in the 'C#' language.
     *
     * @param networkId The network ID.
     * @param nodeId The node for which the processimage to be generated.
     * @param targetPath The location to save the output file.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean buildCSharpProcessImage(String networkId, short nodeId,
            java.nio.file.Path targetPath) throws CoreException {
        String piDataOutput[] = new String[1];
        Result res = OpenConfiguratorCore.GetInstance()
                .BuildNETProcessImage(networkId, nodeId, piDataOutput);
        if (!res.IsSuccessful()) {
            IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    IStatus.OK,
                    OpenConfiguratorLibraryUtils.getErrorMessage(res), null);
            OpenConfiguratorMessageConsole.getInstance().printErrorMessage(
                    OpenConfiguratorLibraryUtils.getErrorMessage(res));
            System.err.println("Build ERR "
                    + OpenConfiguratorLibraryUtils.getErrorMessage(res));
            throw new CoreException(errorStatus);
        } else {
            java.nio.file.Path targetFilePath = targetPath
                    .resolve(IPowerlinkProjectSupport.PROCESSIMAGE_CS);

            try {
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                Files.write(targetFilePath, piDataOutput[0].getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                IStatus errorStatus = new Status(IStatus.ERROR,
                        Activator.PLUGIN_ID, IStatus.OK,
                        "Output file:" + targetFilePath.toString()
                                + " is not accessible.",
                        e);
                throw new CoreException(errorStatus);
            }
        }
        return true;
    }

    /**
     * Build the ProcessImage descriptions for currently active project.
     *
     * @param networkId The network ID.
     * @param outputpath The location to save the output files.
     * @param monitor Monitor instance to update the progress activity.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean buildProcessImageDescriptions(String networkId,
            java.nio.file.Path targetPath, IProgressMonitor monitor)
                    throws CoreException {

        ByteCollection nodeIdCollection = new ByteCollection();
        Result res = OpenConfiguratorCore.GetInstance()
                .GetAvailableNodeIds(networkId, nodeIdCollection);
        if (!res.IsSuccessful()) {
            IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    IStatus.OK,
                    OpenConfiguratorLibraryUtils.getErrorMessage(res), null);
            OpenConfiguratorMessageConsole.getInstance().printErrorMessage(
                    OpenConfiguratorLibraryUtils.getErrorMessage(res));
            System.err.println("Build ERR "
                    + OpenConfiguratorLibraryUtils.getErrorMessage(res));
            throw new CoreException(errorStatus);
        }

        boolean ret = false;
        for (int i = 0; i < nodeIdCollection.size(); i++) {
            short value = nodeIdCollection.get(i);
            java.nio.file.Path processImagePath = targetPath;
            if (value != 240) {
                processImagePath = processImagePath
                        .resolve(String.valueOf(value));
                // NOTE: Remove 'continue' to generate the Individual CN's PI
                // descriptions.
                continue;
            }
            ret = buildCProcessImage(networkId, value, processImagePath);
            ret = buildXmlProcessImage(networkId, value, processImagePath);
            ret = buildCSharpProcessImage(networkId, value, processImagePath);
        }
        return ret;
    }

    /**
     * Writes the processimage variables for the specified node ID. The
     * processimage variable are available in the XML format.
     *
     * @param networkId The network ID.
     * @param nodeId The node for which the processimage to be generated.
     * @param targetPath The location to save the output file.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean buildXmlProcessImage(String networkId, short nodeId,
            java.nio.file.Path targetPath) throws CoreException {
        String piDataOutput[] = new String[1];
        Result res = OpenConfiguratorCore.GetInstance()
                .BuildXMLProcessImage(networkId, nodeId, piDataOutput);
        if (!res.IsSuccessful()) {
            IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    IStatus.OK,
                    OpenConfiguratorLibraryUtils.getErrorMessage(res), null);
            OpenConfiguratorMessageConsole.getInstance().printErrorMessage(
                    OpenConfiguratorLibraryUtils.getErrorMessage(res));
            System.err.println("Build ERR "
                    + OpenConfiguratorLibraryUtils.getErrorMessage(res));
            throw new CoreException(errorStatus);
        } else {
            java.nio.file.Path targetFilePath = targetPath
                    .resolve(IPowerlinkProjectSupport.XAP_XML);

            try {
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                Files.write(targetFilePath, piDataOutput[0].getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                IStatus errorStatus = new Status(IStatus.ERROR,
                        Activator.PLUGIN_ID, IStatus.OK,
                        "Output file:" + targetFilePath.toString()
                                + " is not accessible.",
                        e);
                throw new CoreException(errorStatus);
            }
        }
        return true;
    }

    /**
     * Cleans the generated output files. The list of output files are available
     * in {@link IPowerlinkProjectSupport}
     */
    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {

        List<IndustrialNetworkProjectEditor> projectEditors = getOpenProjectEditors();
        for (IndustrialNetworkProjectEditor pjtEditor : projectEditors) {

            String networkId = pjtEditor.getNetworkId();
            if (getProject().getName().compareTo(networkId) != 0) {
                continue;
            }

            Path outputpath = pjtEditor.getProjectOutputPath();

            java.nio.file.Path targetPath;

            if (outputpath.isLocal()) {
                targetPath = FileSystems.getDefault().getPath(
                        getProject().getLocation().toString(),
                        outputpath.getPath());
            } else {
                targetPath = FileSystems.getDefault()
                        .getPath(outputpath.getPath());
            }

            for (String outputFile : IPowerlinkProjectSupport.OUTPUT_FILES) {
                java.nio.file.Path targetFilePath = targetPath
                        .resolve(outputFile);
                if (Files.exists(targetFilePath, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.delete(targetFilePath);
                    } catch (NoSuchFileException x) {
                        System.err.format(
                                "%s: no such" + " file or directory%n",
                                targetPath);
                    } catch (DirectoryNotEmptyException x) {
                        System.err.format("%s not empty%n", targetPath);
                    } catch (IOException x) {
                        System.err.println(x);
                    }
                }
            }

            getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        System.out.println(
                "Project:" + getProject().getName() + " Clean successful");
    }

    /**
     * Create the mnobd.cdc in the specified output folder.
     *
     * @param outputFolder Location to save the file.
     * @param buffer The file contents.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean createMnobdCdc(java.nio.file.Path outputFolder,
            ByteBuffer buffer) throws CoreException {

        java.nio.file.Path targetFilePath = outputFolder
                .resolve(IPowerlinkProjectSupport.MN_OBD_CDC);

        try {
            Files.write(targetFilePath, buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
            IStatus errorStatus = new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, IStatus.OK, "Output file:"
                            + targetFilePath.toString() + " is not accessible.",
                    e);
            throw new CoreException(errorStatus);
        }

        return true;
    }

    /**
     * Create the mnobd_char.txt in the specified output folder.
     *
     * @param outputFolder Location to save the file.
     * @param buffer The file contents.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean createMnobdHexTxt(java.nio.file.Path outputFolder,
            ByteBuffer buffer) throws CoreException {

        StringBuilder sb = new StringBuilder();
        byte[] txtArray = buffer.array();
        short lineBreakCount = 0;
        for (Byte bs : txtArray) {
            sb.append("0x"); //$NON-NLS-1$
            sb.append(String.format("%02X", bs)); //$NON-NLS-1$
            sb.append(","); //$NON-NLS-1$
            lineBreakCount++;

            if (lineBreakCount == 16) {
                sb.append(System.lineSeparator());
                lineBreakCount = 0;
            } else {
                sb.append(" "); //$NON-NLS-1$
            }
        }

        java.nio.file.Path targetFilePath = outputFolder
                .resolve(IPowerlinkProjectSupport.MN_OBD_CHAR_TXT);

        try {
            Files.write(targetFilePath, sb.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            IStatus errorStatus = new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, IStatus.OK, "Output file:"
                            + targetFilePath.toString() + " is not accessible.",
                    e);
            throw new CoreException(errorStatus);
        }
        return true;
    }

    /**
     * Create the mnobd.txt in the specified output folder.
     *
     * @param outputFolder Location to save the file.
     * @param configuration The file contents.
     * @return <code>True</code> if successful and <code>False</code> otherwise.
     * @throws CoreException
     */
    private boolean createMnobdTxt(java.nio.file.Path outputFolder,
            final String configuration) throws CoreException {

        java.nio.file.Path targetFilePath = outputFolder
                .resolve(IPowerlinkProjectSupport.MN_OBD_TXT);

        try {
            Files.write(targetFilePath, configuration.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            IStatus errorStatus = new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, IStatus.OK, "Output file:"
                            + targetFilePath.toString() + " is not accessible.",
                    e);
            throw new CoreException(errorStatus);
        }

        return true;
    }

    /**
     * Invokes a full build process on the the available projects.
     *
     * @param monitor Monitor instance to update the progress activity.
     * @throws CoreException
     */
    protected void fullBuild(final IProgressMonitor monitor)
            throws CoreException {

        // Auto save all the open editors
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                List<IndustrialNetworkProjectEditor> projectEditors = getOpenProjectEditors();
                for (IndustrialNetworkProjectEditor pjtEditor : projectEditors) {
                    if (pjtEditor.isDirty()) {
                        pjtEditor.doSave(monitor);
                    }
                }
            }
        });

        List<IndustrialNetworkProjectEditor> projectEditors = getOpenProjectEditors();
        for (IndustrialNetworkProjectEditor pjtEditor : projectEditors) {

            final String networkId = pjtEditor.getNetworkId();
            if (getProject().getName().compareTo(networkId) != 0) {
                continue;
            }

            System.out.println("Build Started: Project: " + networkId);
            OpenConfiguratorMessageConsole.getInstance().printInfoMessage(
                    "Build Started for project: " + networkId);
            getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

            long buildStartTime = System.currentTimeMillis();

            Path outputpath = pjtEditor.getProjectOutputPath();

            final java.nio.file.Path targetPath;

            if (outputpath.isLocal()) {
                targetPath = FileSystems.getDefault().getPath(
                        getProject().getLocation().toString(),
                        outputpath.getPath());
            } else {
                targetPath = FileSystems.getDefault()
                        .getPath(outputpath.getPath());
            }

            boolean buildCdcSuccess = buildConciseDeviceConfiguration(networkId,
                    targetPath, monitor);
            if (buildCdcSuccess) {

                boolean buildPiSuccess = buildProcessImageDescriptions(
                        networkId, targetPath, monitor);
                if (!buildPiSuccess) {
                    OpenConfiguratorMessageConsole.getInstance()
                            .printErrorMessage(
                                    "Build failed for project: " + networkId);
                } else {
                    OpenConfiguratorMessageConsole.getInstance()
                            .printInfoMessage(
                                    "Build finished successfully for Project: "
                                            + networkId);
                    OpenConfiguratorMessageConsole.getInstance()
                            .printInfoMessage("Generated output files at: "
                                    + targetPath.toString());
                }

                OpenConfiguratorMessageConsole.getInstance()
                        .printInfoMessage("Updating node configuration files.");

                try {
                    pjtEditor.persistLibraryData(monitor);
                } catch (InterruptedException | InvocationTargetException e) {

                    IStatus errorStatus = new Status(IStatus.ERROR,
                            Activator.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
                    OpenConfiguratorMessageConsole.getInstance()
                            .printErrorMessage(
                                    "Failed to update the node configuration files.\n\tError message: "
                                            + e.getMessage());
                    throw new CoreException(errorStatus);
                }
            } else {
                String errorStr = "Build failed for project: " + networkId;
                OpenConfiguratorMessageConsole.getInstance()
                        .printErrorMessage(errorStr);
                System.err.println(errorStr);
                IStatus errorStatus = new Status(IStatus.ERROR,
                        Activator.PLUGIN_ID, IStatus.OK, errorStr, null);
                throw new CoreException(errorStatus);
            }

            getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

            long buildEndTime = System.currentTimeMillis();
            final long totalTimeInSeconds = (buildEndTime - buildStartTime)
                    / 1000;

            OpenConfiguratorMessageConsole.getInstance().printInfoMessage(
                    "Completed updating node configuration files.");

            System.out
                    .println("Build completed in " + totalTimeInSeconds + "s");
        }
    }
}
