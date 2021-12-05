/*
 * Copyright 2021 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.mypy.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import com.leinardi.pycharm.mypy.util.FileTypes;

import java.util.Collections;

/**
 * Action to execute a Mypy scan on the current editor file.
 */
public class ScanCurrentFile extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return;
        }

        try {
            final MypyPlugin mypyPlugin
                    = project.getComponent(MypyPlugin.class);
            if (mypyPlugin == null) {
                throw new IllegalStateException("Couldn't get mypy plugin");
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);
            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.current");

                    final VirtualFile selectedFile = getSelectedFile(project);
                    if (selectedFile != null) {
                        project.getComponent(MypyPlugin.class).asyncScanFiles(
                                Collections.singletonList(selectedFile));
                    }

                } catch (Throwable e) {
                    MypyPlugin.processErrorAndLog("Current File scan", e);
                }
            });

        } catch (Throwable e) {
            MypyPlugin.processErrorAndLog("Current File scan", e);
        }
    }

    private VirtualFile getSelectedFile(final Project project) {

        VirtualFile selectedFile = null;

        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            selectedFile = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
        }

        if (selectedFile == null) {
            // this is the preferred solution, but it doesn't respect the focus of split editors at present
            final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length > 0) {
                selectedFile = selectedFiles[0];
            }
        }

        // validate selected file against scan scope
        if (selectedFile != null) {
            if (!FileTypes.isPython(selectedFile.getFileType())) {
                selectedFile = null;
            }
        }

        return selectedFile;
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                return;
            }

            final MypyPlugin mypyPlugin
                    = project.getComponent(MypyPlugin.class);
            if (mypyPlugin == null) {
                throw new IllegalStateException("Couldn't get mypy plugin");
            }
            final VirtualFile selectedFile = getSelectedFile(project);

            // disable if no file is selected or scan in progress
            final Presentation presentation = event.getPresentation();
            if (selectedFile != null) {
                presentation.setEnabled(!mypyPlugin.isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        } catch (Throwable e) {
            MypyPlugin.processErrorAndLog("Current File button update", e);
        }
    }
}
