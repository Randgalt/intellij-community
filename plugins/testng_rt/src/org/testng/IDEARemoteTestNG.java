/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.testng;


import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import org.testng.collections.Lists;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class IDEARemoteTestNG extends TestNG {

  private final String myParam;
  public IDEARemoteTestNG(String param) {
    myParam = param;
  }

  private static void calculateAllSuites(List<XmlSuite> suites, List<XmlSuite> outSuites) {
    for (XmlSuite s : suites) {
      outSuites.add(s);
      calculateAllSuites(s.getChildSuites(), outSuites);
    }
  }

   public void run() {
    try {
      initializeSuitesAndJarFile();

      List<XmlSuite> suites = Lists.newArrayList();
      calculateAllSuites(m_suites, suites);
      if(suites.size() > 0) {

        int testCount= 0;

        for (XmlSuite suite : suites) {
          final List<XmlTest> tests = suite.getTests();
          for (XmlTest test : tests) {
            try {
              if (myParam != null) {
                for (XmlClass aClass : test.getXmlClasses()) {
                  for (XmlInclude include : aClass.getIncludedMethods()) {
                    final XmlInclude xmlInclude = new XmlInclude(include.getName(), Collections.singletonList(Integer.parseInt(myParam)), 0);
                    aClass.setIncludedMethods(Collections.singletonList(xmlInclude));
                  }
                }
              }
            }
            catch (NumberFormatException e) {
              System.err.println("Invocation number: expected integer but found: " + myParam);
            }
            testCount += test.getClasses().size();
          }
        }

        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("count", String.valueOf(testCount));
        System.out.println(ServiceMessage.asString("testCount", map));
        final IDEATestNGRemoteListener listener = new IDEATestNGRemoteListener();
        addListener((ISuiteListener)listener);
        addListener((ITestListener)listener);
        super.run();
        System.exit(0);
      }
      else {
        System.out.println("##teamcity[enteredTheMatrix]");
        System.err.println("Nothing found to run");
      }
    }
    catch(Throwable cause) {
      cause.printStackTrace(System.err);
    }
  }
}
