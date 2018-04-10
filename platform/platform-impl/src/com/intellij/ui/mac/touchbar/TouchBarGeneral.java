// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.mac.touchbar;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.mac.foundation.Foundation;
import com.intellij.ui.mac.foundation.ID;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TouchBarGeneral extends TouchBarActionBase {
  private static final Map<Project, TouchBarGeneral> ourInstances = new HashMap<>();

  private final Project   myProject;

  private TBItemButton    myButtonAddRunConf;

  private TBItemPopover   myPopoverRunConf;
  private TouchBar        myPopoverRunConfExpandTB;
  private TBItemButton    myPopoverRunConfTapAndHold;
  private TBItemScrubber  myScrubberRunConf;

  private TBItemAnActionButton    myButtonRun;
  private TBItemAnActionButton    myButtonDebug;
  private TBItemAnActionButton    myButtonStop;

  private TouchBarGeneral(Project project) {
    super("general");
    myProject = project;

    addAnActionButton("CompileDirty", false); // NOTE: IdeActions.ACTION_COMPILE doesn't work

    myButtonAddRunConf = addButton(AllIcons.General.Add, "Add Configuration", IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS);

    final TouchBar tapHoldTB = new TouchBar("run_configs_popover_tap_and_hold");
    tapHoldTB.addFlexibleSpacing();
    myPopoverRunConfTapAndHold = tapHoldTB.addButton(null, null, (NSTLibrary.Action)null);
    tapHoldTB.selectVisibleItemsToShow();

    myPopoverRunConfExpandTB = new TouchBar("run_configs_popover_expand");
    myPopoverRunConfExpandTB.addButton(AllIcons.Actions.EditSource, null, IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS);
    myScrubberRunConf = myPopoverRunConfExpandTB.addScrubber(500);
    myPopoverRunConfExpandTB.addFlexibleSpacing();
    myPopoverRunConfExpandTB.selectVisibleItemsToShow();

    myPopoverRunConf = addPopover(null, null, 143, myPopoverRunConfExpandTB, tapHoldTB);

    myButtonRun = addAnActionButton(IdeActions.ACTION_DEFAULT_RUNNER);
    myButtonDebug = addAnActionButton(IdeActions.ACTION_DEFAULT_DEBUGGER);
    myButtonStop = addAnActionButton(IdeActions.ACTION_STOP_PROGRAM);
    myButtonRun.setAutoVisibility(false);
    myButtonDebug.setAutoVisibility(false);
    myButtonStop.setAutoVisibility(false);

    addSpacing(true);
    addAnActionButton("Vcs.UpdateProject", false);  // NOTE: IdeActions.ACTION_CVS_CHECKOUT doesn't works
    addAnActionButton("CheckinProject", false);     // NOTE: IdeActions.ACTION_CVS_COMMIT doesn't works

    _updateRunConfigs();

    final MessageBus mb = myProject.getMessageBus();

    mb.connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
      @Override
      public void runConfigurationSelected() {
        _updateSelectedConf();
      }
      @Override
      public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
        _updateRunConfigs();
      }
      @Override
      public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        _updateRunConfigs();
      }
    });
  }

  public TouchBar getPopoverRunConfExpandTB() { return myPopoverRunConfExpandTB; }

  public static @NotNull TouchBarGeneral instance(@NotNull Project project) {
    // NOTE: called from EDT only
    TouchBarGeneral result = ourInstances.get(project);
    if (result == null) {
      final ID pool = Foundation.invoke("NSAutoreleasePool", "new");
      try {
        result = new TouchBarGeneral(project);
        ourInstances.put(project, result);
      } finally {
        Foundation.invoke(pool, "release");
      }
    }
    return result;
  }

  public static void release(@NotNull Project project) {
    // NOTE: called from EDT only
    TouchBarGeneral result = ourInstances.remove(project);
    if (result != null)
      result.release();
  }

  private void _updateSelectedConf() {
    final Icon icon;
    final String text;
    final RunManager rm = RunManager.getInstance(myProject);
    final RunnerAndConfigurationSettings selected = rm.getSelectedConfiguration();
    if (selected != null) {
      icon = selected.getConfiguration().getIcon();
      text = selected.getName();
    } else {
      icon = null;
      text = "Select configuration";
    }

    myPopoverRunConf.update(icon, text);
    myPopoverRunConfTapAndHold.update(icon, text);
  }

  private void _updateRunConfigs() {
    final RunManager rm = RunManager.getInstance(myProject);
    final List<RunnerAndConfigurationSettings> allRunCongigs = rm.getAllSettings();
    if (allRunCongigs == null || allRunCongigs.isEmpty()) {
      myButtonAddRunConf.setVisible(true);
      myPopoverRunConf.setVisible(false);
      myButtonRun.setVisible(false);
      myButtonDebug.setVisible(false);
      myButtonStop.setVisible(false);
    } else {
      myButtonAddRunConf.setVisible(false);
      myPopoverRunConf.setVisible(true);
      myButtonRun.setVisible(true);
      myButtonDebug.setVisible(true);
      myButtonStop.setVisible(true);

      _updateSelectedConf();

      List<TBItemScrubber.ItemData> scrubberItems = new ArrayList<>();
      for (RunnerAndConfigurationSettings rc : allRunCongigs) {
        final Icon iconRc = rc.getConfiguration().getIcon();
        scrubberItems.add(new TBItemScrubber.ItemData(iconRc, rc.getName(), () -> {
          // NOTE: executed at AppKit-thread
          ApplicationManager.getApplication().invokeLater(()->{ rm.setSelectedConfiguration(rc); });
          myPopoverRunConf.dismiss();
        }));
      }
      myScrubberRunConf.setItems(scrubberItems);
    }
    selectVisibleItemsToShow();
  }
}
