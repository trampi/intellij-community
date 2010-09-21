/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration.classpath;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Icons;
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * @author nik
 */
public class ProjectStructureChooseLibrariesDialog extends ChooseLibrariesFromTablesDialog {
  private StructureConfigurableContext myContext;
  private Condition<Library> myAcceptedLibraries;
  private AddNewLibraryItemAction myNewLibraryAction;

  public ProjectStructureChooseLibrariesDialog(JComponent parentComponent,
                                               @Nullable Project project,
                                               StructureConfigurableContext context,
                                               Condition<Library> acceptedLibraries, AddNewLibraryItemAction newLibraryAction) {
    super(parentComponent, "Choose Libraries", project, true);
    myContext = context;
    myAcceptedLibraries = acceptedLibraries;
    myNewLibraryAction = newLibraryAction;
    setOKButtonText("Add Selected");
    init();
  }

  @NotNull
  @Override
  protected Library[] getLibraries(@NotNull LibraryTable table) {
    final LibrariesModifiableModel model = getLibrariesModifiableModel(table);
    if (model == null) return Library.EMPTY_ARRAY;
    return model.getLibraries();
  }

  private LibrariesModifiableModel getLibrariesModifiableModel(LibraryTable table) {
    return myContext.myLevel2Providers.get(table.getTableLevel());
  }

  @Override
  protected boolean acceptsElement(Object element) {
    if (element instanceof Library) {
      final Library library = (Library)element;
      return myAcceptedLibraries.value(library);
    }
    return true;
  }

  @NotNull
  private String getLibraryName(@NotNull Library library) {
    final LibrariesModifiableModel model = getLibrariesModifiableModel(library.getTable());
    if (model != null) {
      if (model.hasLibraryEditor(library)) {
        return model.getLibraryEditor(library).getName();
      }
    }
    return library.getName();
  }

  @Override
  protected Action[] createActions() {
    return new Action[]{getCancelAction()};
  }

  @Override
  protected Action[] createLeftSideActions() {
    return new Action[]{getOKAction(), new CreateNewLibraryAction()};
  }

  @Override
  protected LibrariesTreeNodeBase<Library> createLibraryDescriptor(NodeDescriptor parentDescriptor,
                                                                   Library library) {
    final String libraryName = getLibraryName(library);
    return new LibraryEditorDescriptor(getProject(), parentDescriptor, library, libraryName, myContext);
  }

  private static class LibraryEditorDescriptor extends LibrariesTreeNodeBase<Library> {
    protected LibraryEditorDescriptor(final Project project, final NodeDescriptor parentDescriptor, final Library element,
                                      String libraryName, StructureConfigurableContext context) {
      super(project, parentDescriptor, element);
      final PresentationData templatePresentation = getTemplatePresentation();
      Icon icon = LibraryPresentationManager.getInstance().getCustomIcon(element, context);
      if (icon == null) {
        icon = Icons.LIBRARY_ICON;
      }
      templatePresentation.setIcons(icon);
      templatePresentation.addText(libraryName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
  }

  private class CreateNewLibraryAction extends DialogWrapperAction {
    private CreateNewLibraryAction() {
      super("New Library...");
      putValue(MNEMONIC_KEY, KeyEvent.VK_N);
    }

    @Override
    protected void doAction(ActionEvent e) {
      close(CANCEL_EXIT_CODE);
      myNewLibraryAction.execute();
    }
  }
}
