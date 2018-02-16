/*
 * Copyright 2014 - 2018 Herb Bowie
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
package com.powersurgepub.headout;

  import com.powersurgepub.psutils2.textio.*;

  import javafx.scene.layout.*;

/**
 Standard interface for a transform routine used by Headout. 

 @author Herb Bowie
 */
public interface HeadOutTransformer {

  /**
   Get the GridPane containing the controls for this transformer.

   @return the grid pane containing the controls for this type of transformation.
   */
  public GridPane getGrid();

  /**
   Transform the input to the output.

   @param reader The input.
   @param writer The ouput.
   @throws TransformException If something goes amiss.
   */
  public void transformNow(TextLineReader reader, TextLineWriter writer)
      throws TransformException;
  
  /**
   Save user options as preferences. 
  */
  public void savePrefs();
  
}
