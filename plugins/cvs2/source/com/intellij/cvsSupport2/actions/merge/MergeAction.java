package com.intellij.cvsSupport2.actions.merge;

import com.intellij.cvsSupport2.CvsUtil;
import com.intellij.cvsSupport2.actions.actionVisibility.CvsActionVisibility;
import com.intellij.cvsSupport2.actions.cvsContext.CvsContext;
import com.intellij.cvsSupport2.actions.cvsContext.CvsContextWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MergeAction extends AnAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.cvsSupport2.actions.merge.MergeAction");
  private final CvsActionVisibility myVisibility = new CvsActionVisibility();

  public MergeAction() {
    myVisibility.shouldNotBePerformedOnDirectory();
    myVisibility.canBePerformedOnSeveralFiles();
    myVisibility.addCondition(new CvsActionVisibility.Condition() {
      public boolean isPerformedOn(CvsContext context) {
        VirtualFile[] files = context.getSelectedFiles();
        for(VirtualFile file: files) {
          FileStatus status = FileStatusManager.getInstance(context.getProject()).getStatus(file);
          if (status != FileStatus.MERGE && status != FileStatus.MERGED_WITH_CONFLICTS) {
            return false;
          }
        }
        return true;
      }
    });
  }

  public void actionPerformed(AnActionEvent e) {
    try {

      final VcsContext context = CvsContextWrapper.createCachedInstance(e);
      final VirtualFile[] files = context.getSelectedFiles();
      if (files == null || files.length == 0) return;
      final ReadonlyStatusHandler.OperationStatus operationStatus =
        ReadonlyStatusHandler.getInstance(context.getProject()).ensureFilesWritable(files);
      if (operationStatus.hasReadonlyFiles()) {
        return;
      }
      final Map<VirtualFile, List<String>> fileToRevisions = new HashMap<VirtualFile, List<String>>();
      for(VirtualFile file: files) {
        fileToRevisions.put(file, CvsUtil.getAllRevisionsForFile(file));
      }
      final Project project = context.getProject();
      AbstractVcsHelper.getInstance(project).showMergeDialog(new ArrayList<VirtualFile>(fileToRevisions.keySet()),
                                                             new CvsMergeProvider(fileToRevisions, project));

    }
    catch (Exception e1) {
      LOG.error(e1);
    }
  }

  public void update(AnActionEvent e) {
    myVisibility.applyToEvent(e);
  }

}
