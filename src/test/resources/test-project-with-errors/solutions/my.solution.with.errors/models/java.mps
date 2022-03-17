<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:098fce55-4c9a-4d06-84b9-04b5ca0d51c6(my.solution.with.errors.java)">
  <persistence version="9" />
  <languages>
    <use id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage" version="11" />
  </languages>
  <imports>
    <import index="ggvx" ref="r:a73d36b1-a672-4ac9-b2ff-9846b544fa43(my.solution.java)" />
  </imports>
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1070462154015" name="jetbrains.mps.baseLanguage.structure.StaticFieldDeclaration" flags="ig" index="Wx3nA" />
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu" />
      <concept id="1107461130800" name="jetbrains.mps.baseLanguage.structure.Classifier" flags="ng" index="3pOWGL">
        <child id="5375687026011219971" name="member" index="jymVt" unordered="true" />
      </concept>
      <concept id="1178549954367" name="jetbrains.mps.baseLanguage.structure.IVisible" flags="ng" index="1B3ioH">
        <child id="1178549979242" name="visibility" index="1B3o_S" />
      </concept>
      <concept id="1146644602865" name="jetbrains.mps.baseLanguage.structure.PublicVisibility" flags="nn" index="3Tm1VV" />
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ng" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
  </registry>
  <node concept="312cEu" id="20vAV8Q$3Zm">
    <property role="TrG5h" value="ClassWithCheckErrors" />
    <node concept="Wx3nA" id="20vAV8Q$45B" role="jymVt">
      <property role="TrG5h" value="missingTypeField" />
      <node concept="3Tm1VV" id="20vAV8Q$44w" role="1B3o_S" />
    </node>
    <node concept="3Tm1VV" id="20vAV8Q$3Zn" role="1B3o_S" />
  </node>
</model>

