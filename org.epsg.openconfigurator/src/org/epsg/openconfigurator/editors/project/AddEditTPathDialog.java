/*******************************************************************************
 * @file   AddEditTPathDialog.java
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

package org.epsg.openconfigurator.editors.project;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epsg.openconfigurator.util.OpenCONFIGURATORProjectUtils;
import org.epsg.openconfigurator.xmlbinding.projectfile.TPath;
import org.epsg.openconfigurator.xmlbinding.projectfile.TProjectConfiguration;

/**
 * A dialog to modify the path settings of the project XML.
 *
 * @author Ramakrishnan P
 *
 */
public final class AddEditTPathDialog extends TitleAreaDialog {

  private boolean dirty = false;
  private TPath path = new TPath();
  private TProjectConfiguration.PathSettings pathSettingsModel;

  private final static String NAME_LABEL = "Name:";
  private final static String LOCATION_LABEL = "Location:";
  private final static String BROWSE_LABEL = "Browse...";
  private final static String BROWSE_LOCATION_TITLE = "Choose the path for the generated output files";

  private final static String DIALOG_TITLE = "Add Location";
  private final static String DIALOG_DEFAULT_MESSAGE = "Choose the path for the generated output files";

  private final static String NAME_FIELD_MESSAGE_EMPTY_NAME = "Enter any name to identify the location";
  private final static String NAME_FIELD_MESSAGE_NAME_ALREADY_EXISTS = "The name already exists!";
  private final static String NAME_FIELD_MESSAGE_NAME_INVALID = "Enter a valid name";

  private final static String LOCATION_FIELD_MESSAGE_MUST_SPEFIFY_PATH = "Must specify a location.";
  private final static String LOCATION_FIELD_MESSAGE_INVALID_PATH = "Choose a valid location for the output files";

  /** UI Controls */
  private Text txtName;
  private Text txtLocation;

  /**
   * Create the Add/Edit Path settings dialog.
   *
   * @param parentShell
   */
  public AddEditTPathDialog(Shell parentShell,
      TProjectConfiguration.PathSettings pathSettingsModel, final TPath path) {
    super(parentShell);

    if (path != null) {
      this.path = path;
    }

    this.pathSettingsModel = pathSettingsModel;
  }

  /**
   * Create contents of the dialog.
   *
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    setTitle(DIALOG_TITLE);
    setMessage(DIALOG_DEFAULT_MESSAGE);

    Composite container = new Composite(parent, SWT.NONE);
    container.setLayoutData(new GridData(GridData.FILL_BOTH));
    container.setLayout(new GridLayout(3, false));

    Label lblSettingsName = new Label(container, SWT.CENTER);
    lblSettingsName.setText(NAME_LABEL);
    lblSettingsName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

    txtName = new Text(container, SWT.NONE);
    txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    if (path != null && path.getId() != null) {
      txtName.setText(path.getId());
    }
    txtName.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {

        if (txtName.getText().isEmpty()) {
          setErrorMessage(NAME_FIELD_MESSAGE_EMPTY_NAME);
        }

        // Check if is ID already present in the model
        if (OpenCONFIGURATORProjectUtils.isPathIdAlreadyPresent(pathSettingsModel,
            txtName.getText())) {
          setErrorMessage(NAME_FIELD_MESSAGE_NAME_ALREADY_EXISTS);
        } else {
          setErrorMessage(null);
        }
        dirty = true;
      }
    });

    Label lblValue = new Label(container, SWT.CENTER);
    lblValue.setText(LOCATION_LABEL);
    lblValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

    txtLocation = new Text(container, SWT.NONE);
    txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    txtLocation.setText("");
    if (path != null && path.getPath() != null) {
      txtLocation.setText(path.getPath());
    }
    txtLocation.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (txtLocation.getText().isEmpty()) {
          setErrorMessage(LOCATION_FIELD_MESSAGE_MUST_SPEFIFY_PATH);
        } else {
          setErrorMessage(null);
        }

        // TODO: Check for the location is valid.

        dirty = true;
      }
    });

    Button btnBrowse = new Button(container, SWT.PUSH);
    btnBrowse.setText(BROWSE_LABEL);
    GridData gd = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
    btnBrowse.setLayoutData(gd);
    btnBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {

        DirectoryDialog dirDialog = new DirectoryDialog(getShell(), SWT.OPEN);
        dirDialog.setText(BROWSE_LOCATION_TITLE);
        if (!txtLocation.getText().isEmpty()) {
          dirDialog.setFilterPath(txtLocation.getText());
        }

        // Open Dialog and save result of selection

        String selectedOutputPath = dirDialog.open();
        if (selectedOutputPath != null && !selectedOutputPath.isEmpty()) {
          // path.setPath(selectedOutputPath);
          txtLocation.setText(selectedOutputPath);
          dirty = true;
        }
      }
    });

    return container;
  }

  /**
   * Create contents of the footer button bar.
   *
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  /**
   * Handles the button pressed events for the buttons in the footer of the dialog.
   */
  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      if (isPageComplete()) {
        path.setId(txtName.getText());
        path.setPath(txtLocation.getText());
        okPressed();
      }

    } else if (buttonId == IDialogConstants.CANCEL_ID) {
      cancelPressed();
    }
  }

  /**
   * Validates the input from the dialog controls.
   *
   * @return Returns true if the inputs are valid, false otherwise.
   */
  private boolean isPageComplete() {
    boolean nameValid = false;
    boolean locationValid = false;

    if (txtName.getText() != null
        && !txtName.getText().isEmpty()
        && !OpenCONFIGURATORProjectUtils.isPathIdAlreadyPresent(pathSettingsModel,
            txtName.getText())) {
      nameValid = true;
      setErrorMessage(null);
    } else {
      setErrorMessage(NAME_FIELD_MESSAGE_NAME_INVALID);
    }

    if (txtLocation.getText() != null && !txtLocation.getText().isEmpty()) {
      locationValid = true;
    } else {
      setErrorMessage(LOCATION_FIELD_MESSAGE_INVALID_PATH);
    }

    return (nameValid && locationValid);
  }

  /**
   * Return the initial size of the dialog.
   */
  @Override
  protected Point getInitialSize() {
    return new Point(450, 350);
  }

  /**
   * Checks for the dialog's dirty state.
   *
   * @return true if the data is modified, false otherwise.
   */
  public boolean isDirty() {
    return dirty;
  }

}