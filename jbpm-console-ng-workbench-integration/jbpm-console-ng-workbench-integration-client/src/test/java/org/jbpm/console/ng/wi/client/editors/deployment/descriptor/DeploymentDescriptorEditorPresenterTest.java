/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.jbpm.console.ng.wi.client.editors.deployment.descriptor;

import java.util.ArrayList;

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.gwtbootstrap3.client.ui.Modal;
import org.jbpm.console.ng.wi.dd.model.DeploymentDescriptorModel;
import org.jbpm.console.ng.wi.dd.service.DDEditorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.history.VersionRecordManager;
import org.uberfire.java.nio.IOException;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mvp.Command;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({Modal.class})
public class DeploymentDescriptorEditorPresenterTest {

    @Mock
    DDEditorService ddEditorService;

    DeploymentDescriptorEditorPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new DeploymentDescriptorEditorPresenter(mock(DeploymentDescriptorView.class)) {
            {
                ddEditorService = new CallerMock<>(DeploymentDescriptorEditorPresenterTest.this.ddEditorService);
                versionRecordManager = mock(VersionRecordManager.class);
            }
        };
    }

    @Test
    public void commandIsCalled() throws Exception {

        doReturn(new ArrayList<ValidationMessage>()).when(ddEditorService).validate(any(Path.class),
                                                                                    any(DeploymentDescriptorModel.class));

        final Command afterValidation = mock(Command.class);
        presenter.onValidate(afterValidation);
        verify(afterValidation).execute();
    }

    @Test
    public void callFailsAndCommandIsCalled() throws Exception {

        doThrow(new IOException()).when(ddEditorService).validate(any(Path.class),
                                                                  any(DeploymentDescriptorModel.class));

        final Command afterValidation = mock(Command.class);
        presenter.onValidate(afterValidation);
        verify(afterValidation).execute();
    }
}