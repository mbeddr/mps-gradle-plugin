<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:af6f35d8-185e-4ea3-b48c-f8a20dc88a20(NewSolution.myModel)">
  <persistence version="9" />
  <languages>
    <devkit ref="fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)" />
  </languages>
  <imports>
    <import index="z1c3" ref="6ed54515-acc8-4d1e-a16c-9fd6cfe951ea/java:jetbrains.mps.project(MPS.Core/)" />
    <import index="wyt6" ref="6354ebe7-c22a-4a0f-ac54-50b52ab9b065/java:java.lang(JDK/)" implicit="true" />
    <import index="guwi" ref="6354ebe7-c22a-4a0f-ac54-50b52ab9b065/java:java.io(JDK/)" implicit="true" />
  </imports>
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1202948039474" name="jetbrains.mps.baseLanguage.structure.InstanceMethodCallOperation" flags="nn" index="liA8E" />
      <concept id="1465982738277781862" name="jetbrains.mps.baseLanguage.structure.PlaceholderMember" flags="nn" index="2tJIrI" />
      <concept id="1154032098014" name="jetbrains.mps.baseLanguage.structure.AbstractLoopStatement" flags="nn" index="2LF5Ji">
        <child id="1154032183016" name="body" index="2LFqv$" />
      </concept>
      <concept id="1197027756228" name="jetbrains.mps.baseLanguage.structure.DotExpression" flags="nn" index="2OqwBi">
        <child id="1197027771414" name="operand" index="2Oq$k0" />
        <child id="1197027833540" name="operation" index="2OqNvi" />
      </concept>
      <concept id="1070475926800" name="jetbrains.mps.baseLanguage.structure.StringLiteral" flags="nn" index="Xl_RD">
        <property id="1070475926801" name="value" index="Xl_RC" />
      </concept>
      <concept id="1081236700938" name="jetbrains.mps.baseLanguage.structure.StaticMethodDeclaration" flags="ig" index="2YIFZL" />
      <concept id="1070533707846" name="jetbrains.mps.baseLanguage.structure.StaticFieldReference" flags="nn" index="10M0yZ">
        <reference id="1144433057691" name="classifier" index="1PxDUh" />
      </concept>
      <concept id="1070534760951" name="jetbrains.mps.baseLanguage.structure.ArrayType" flags="in" index="10Q1$e">
        <child id="1070534760952" name="componentType" index="10Q1$1" />
      </concept>
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu" />
      <concept id="1068498886296" name="jetbrains.mps.baseLanguage.structure.VariableReference" flags="nn" index="37vLTw">
        <reference id="1068581517664" name="variableDeclaration" index="3cqZAo" />
      </concept>
      <concept id="1068498886292" name="jetbrains.mps.baseLanguage.structure.ParameterDeclaration" flags="ir" index="37vLTG" />
      <concept id="1225271177708" name="jetbrains.mps.baseLanguage.structure.StringType" flags="in" index="17QB3L" />
      <concept id="4972933694980447171" name="jetbrains.mps.baseLanguage.structure.BaseVariableDeclaration" flags="ng" index="19Szcq">
        <child id="5680397130376446158" name="type" index="1tU5fm" />
      </concept>
      <concept id="1068580123132" name="jetbrains.mps.baseLanguage.structure.BaseMethodDeclaration" flags="ng" index="3clF44">
        <child id="1068580123133" name="returnType" index="3clF45" />
        <child id="1068580123134" name="parameter" index="3clF46" />
        <child id="1068580123135" name="body" index="3clF47" />
      </concept>
      <concept id="1068580123155" name="jetbrains.mps.baseLanguage.structure.ExpressionStatement" flags="nn" index="3clFbF">
        <child id="1068580123156" name="expression" index="3clFbG" />
      </concept>
      <concept id="1068580123136" name="jetbrains.mps.baseLanguage.structure.StatementList" flags="sn" stub="5293379017992965193" index="3clFbS">
        <child id="1068581517665" name="statement" index="3cqZAp" />
      </concept>
      <concept id="1068581517677" name="jetbrains.mps.baseLanguage.structure.VoidType" flags="in" index="3cqZAl" />
      <concept id="1204053956946" name="jetbrains.mps.baseLanguage.structure.IMethodCall" flags="ng" index="1ndlxa">
        <reference id="1068499141037" name="baseMethodDeclaration" index="37wK5l" />
        <child id="1068499141038" name="actualArgument" index="37wK5m" />
      </concept>
      <concept id="1107461130800" name="jetbrains.mps.baseLanguage.structure.Classifier" flags="ng" index="3pOWGL">
        <child id="5375687026011219971" name="member" index="jymVt" unordered="true" />
      </concept>
      <concept id="1107535904670" name="jetbrains.mps.baseLanguage.structure.ClassifierType" flags="in" index="3uibUv">
        <reference id="1107535924139" name="classifier" index="3uigEE" />
      </concept>
      <concept id="1178549954367" name="jetbrains.mps.baseLanguage.structure.IVisible" flags="ng" index="1B3ioH">
        <child id="1178549979242" name="visibility" index="1B3o_S" />
      </concept>
      <concept id="1146644602865" name="jetbrains.mps.baseLanguage.structure.PublicVisibility" flags="nn" index="3Tm1VV" />
    </language>
    <language id="760a0a8c-eabb-4521-8bfd-65db761a9ba3" name="jetbrains.mps.baseLanguage.logging">
      <concept id="2034914114981261497" name="jetbrains.mps.baseLanguage.logging.structure.LogLowLevelStatement" flags="ng" index="RRSsy">
        <property id="2034914114981261751" name="severity" index="RRSoG" />
        <child id="2034914114981261753" name="message" index="RRSoy" />
      </concept>
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ng" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
    <language id="83888646-71ce-4f1c-9c53-c54016f6ad4f" name="jetbrains.mps.baseLanguage.collections">
      <concept id="1153943597977" name="jetbrains.mps.baseLanguage.collections.structure.ForEachStatement" flags="nn" index="2Gpval">
        <child id="1153944400369" name="variable" index="2Gsz3X" />
        <child id="1153944424730" name="inputSequence" index="2GsD0m" />
      </concept>
      <concept id="1153944193378" name="jetbrains.mps.baseLanguage.collections.structure.ForEachVariable" flags="nr" index="2GrKxI" />
      <concept id="1153944233411" name="jetbrains.mps.baseLanguage.collections.structure.ForEachVariableReference" flags="nn" index="2GrUjf">
        <reference id="1153944258490" name="variable" index="2Gs0qQ" />
      </concept>
    </language>
  </registry>
  <node concept="312cEu" id="1wd9AaS6sTY">
    <property role="TrG5h" value="MyClass" />
    <node concept="2YIFZL" id="1wd9AaS6w0$" role="jymVt">
      <property role="TrG5h" value="main" />
      <node concept="37vLTG" id="1wd9AaS6w0_" role="3clF46">
        <property role="TrG5h" value="args" />
        <node concept="10Q1$e" id="1wd9AaS6w0A" role="1tU5fm">
          <node concept="17QB3L" id="1wd9AaS6w0B" role="10Q1$1" />
        </node>
      </node>
      <node concept="3cqZAl" id="1wd9AaS6w0C" role="3clF45" />
      <node concept="3Tm1VV" id="1wd9AaS6w0D" role="1B3o_S" />
      <node concept="3clFbS" id="1wd9AaS6w0E" role="3clF47">
        <node concept="3clFbF" id="1wd9AaS6w62" role="3cqZAp">
          <node concept="2OqwBi" id="1wd9AaS6w5Z" role="3clFbG">
            <node concept="10M0yZ" id="1wd9AaS6w60" role="2Oq$k0">
              <ref role="1PxDUh" to="wyt6:~System" resolve="System" />
              <ref role="3cqZAo" to="wyt6:~System.out" resolve="out" />
            </node>
            <node concept="liA8E" id="1wd9AaS6w61" role="2OqNvi">
              <ref role="37wK5l" to="guwi:~PrintStream.println(java.lang.String)" resolve="println" />
              <node concept="Xl_RD" id="1wd9AaS6x_Z" role="37wK5m">
                <property role="Xl_RC" value="Hello World!" />
              </node>
            </node>
          </node>
        </node>
      </node>
    </node>
    <node concept="2tJIrI" id="6SsfRvSJOA0" role="jymVt" />
    <node concept="2YIFZL" id="6SsfRvSJQ_P" role="jymVt">
      <property role="TrG5h" value="onlyProject" />
      <node concept="3clFbS" id="6SsfRvSJQ_S" role="3clF47">
        <node concept="RRSsy" id="6SsfRvSJSNm" role="3cqZAp">
          <property role="RRSoG" value="h1akgim/info" />
          <node concept="37vLTw" id="6SsfRvSJSUN" role="RRSoy">
            <ref role="3cqZAo" node="6SsfRvSJSI8" resolve="project" />
          </node>
        </node>
      </node>
      <node concept="3Tm1VV" id="6SsfRvSJQp0" role="1B3o_S" />
      <node concept="3cqZAl" id="6SsfRvSJQ$o" role="3clF45" />
      <node concept="37vLTG" id="6SsfRvSJSI8" role="3clF46">
        <property role="TrG5h" value="project" />
        <node concept="3uibUv" id="6SsfRvSJSI7" role="1tU5fm">
          <ref role="3uigEE" to="z1c3:~Project" resolve="Project" />
        </node>
      </node>
    </node>
    <node concept="2tJIrI" id="6SsfRvSJSnL" role="jymVt" />
    <node concept="2YIFZL" id="nRJS0RMQyI" role="jymVt">
      <property role="TrG5h" value="projectAndArgs" />
      <node concept="3clFbS" id="nRJS0RMQyK" role="3clF47">
        <node concept="RRSsy" id="5Csmp2j9mXf" role="3cqZAp">
          <property role="RRSoG" value="h1akgim/info" />
          <node concept="37vLTw" id="5Csmp2j9nnN" role="RRSoy">
            <ref role="3cqZAo" node="nRJS0RMQz1" resolve="project" />
          </node>
        </node>
        <node concept="2Gpval" id="nRJS0RMQyQ" role="3cqZAp">
          <node concept="2GrKxI" id="nRJS0RMQyR" role="2Gsz3X">
            <property role="TrG5h" value="arg" />
          </node>
          <node concept="37vLTw" id="nRJS0RMQyS" role="2GsD0m">
            <ref role="3cqZAo" node="nRJS0RMQz3" resolve="args" />
          </node>
          <node concept="3clFbS" id="nRJS0RMQyT" role="2LFqv$">
            <node concept="RRSsy" id="5Csmp2j9nGa" role="3cqZAp">
              <property role="RRSoG" value="h1akgim/info" />
              <node concept="2GrUjf" id="5Csmp2j9nPl" role="RRSoy">
                <ref role="2Gs0qQ" node="nRJS0RMQyR" resolve="arg" />
              </node>
            </node>
          </node>
        </node>
      </node>
      <node concept="3cqZAl" id="nRJS0RMQz0" role="3clF45" />
      <node concept="37vLTG" id="nRJS0RMQz1" role="3clF46">
        <property role="TrG5h" value="project" />
        <node concept="3uibUv" id="nRJS0RMQz2" role="1tU5fm">
          <ref role="3uigEE" to="z1c3:~Project" resolve="Project" />
        </node>
      </node>
      <node concept="37vLTG" id="nRJS0RMQz3" role="3clF46">
        <property role="TrG5h" value="args" />
        <node concept="10Q1$e" id="nRJS0RMQz4" role="1tU5fm">
          <node concept="17QB3L" id="nRJS0RMQz5" role="10Q1$1" />
        </node>
      </node>
      <node concept="3Tm1VV" id="nRJS0RMQyZ" role="1B3o_S" />
    </node>
    <node concept="3Tm1VV" id="1wd9AaS6sTZ" role="1B3o_S" />
  </node>
</model>

