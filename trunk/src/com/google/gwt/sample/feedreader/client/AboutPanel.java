/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.feedreader.client;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * A simple about panel.
 */
public class AboutPanel extends WallToWallPanel {
  public AboutPanel(WallToWallPanel parent) {
    super("About...", parent);
    addStyleName("AboutPanel");

    HorizontalPanel hp = new HorizontalPanel();
    hp.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

    AbstractImagePrototype logo = Images.INSTANCE.logo();
    UnsunkImage logoLabel = new UnsunkImage(Resources.INSTANCE.logo());
    hp.add(logoLabel);
    hp.setCellWidth(logoLabel, logo.createImage().getWidth() + "px");

    hp.add(new UnsunkLabel("GWT Feed Reader<br/>"
        + "<div class=\"snippit\">An RSS reader using the "
        + "Google AJAX Feed API and the Google Web Toolkit</div>", true));
    add(new PanelLabel(hp, null));
  }

  protected String getShortTitle() {
    return "About";
  }
}
