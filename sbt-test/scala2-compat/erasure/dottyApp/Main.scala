object Main {
  def main(args: Array[String]): Unit = {
    val z = new scala2Lib.Z

    def dummy[T]: T = null.asInstanceOf[T]

    // Commented out lines intentionally do not typecheck and are
    // tested in changes/Main.scala
    z.a_01(dummy)
    z.a_02(dummy)
    z.b_02X(dummy)
    z.a_03(dummy)
    // z.b_04(dummy)
    // z.b_04X(dummy)
    // z.b_05(dummy)
    z.a_06(dummy)
    z.a_07(dummy)
    z.b_08(dummy)
    z.a_09(dummy)
    z.b_10(dummy)
    z.b_11(dummy)
    z.b_12(dummy)
    z.d_13(dummy)
    z.d_14(dummy)
    z.d_15(dummy)
    z.d_15b(dummy)
    z.d_16(dummy)
    z.d_16b(dummy)
    z.d_17(dummy)
    z.d_18(dummy)
    z.a_19(dummy)
    z.d_19x(dummy)
    z.z_20(dummy)
    z.a_21(dummy)
    z.a_22(dummy)
    z.z_23(dummy)
    z.z_24(dummy)
    z.b_25(dummy)
    z.a_26(dummy)
    z.a_27(dummy)
    z.a_28(dummy)
    z.f_29(dummy)
    z.f_30(dummy)
    z.f_31(dummy)
    z.f_32(dummy)
    z.f_33(dummy)
    z.f_34(dummy)
    z.a_35(dummy)
    z.d_36(dummy)
    z.z_37(dummy)
    z.z_38(dummy)
    z.c_39(dummy)
    z.c_40(dummy)
    z.c_41(dummy)
    z.c_42(dummy)
    //z.b_43(dummy)
    z.c_44(dummy)
    z.c_45(dummy)
    //z.b_46(dummy)
    z.c_47(dummy)
    // z.a_48(dummy)
    // z.c_49(dummy)
    z.c_50(dummy)
    // z.a_51(dummy)
    z.c_52(dummy)
    // z.a_53(dummy)
    z.c_54(dummy)
    z.b_55(dummy)
    // z.b_56(dummy)
    // z.a_57(dummy)
    z.int_58(1)
    z.int_59(1)
    z.int_60(1)
    z.int_61(1)
    z.int_62(1)
    z.int_63(1)
    z.intARRAY_64(dummy)
    z.object_65(dummy)
    z.object_66(dummy)
    z.object_67(dummy)
    z.object_68(dummy)
    z.object_69(dummy)
    z.object_70(dummy)
    z.object_71(dummy)
    z.stringARRAY_72(dummy)
    z.stringARRAY_73(dummy)
    z.stringARRAY_74(dummy)
    z.stringARRAY_75(dummy)
    z.stringARRAY_76(dummy)
    z.stringARRAY_77(dummy)
    z.stringARRAY_78(dummy)
    z.stringARRAY_79(dummy)
    z.object_80(dummy)
    z.object_81(dummy)
    z.objectARRAY_82(dummy)
    z.object_83(dummy)
    z.object_84(dummy)
    z.object_85(dummy)
    z.aARRAY_86(dummy)
    z.aARRAY_87(dummy)
    z.objectARRAY_88(dummy)
    z.objectARRAY_89(dummy)
    z.objectARRAY_90(dummy)
    z.objectARRAY_91(dummy)
    z.objectARRAY_92(dummy)
    z.objectARRAY_93(dummy)
    z.objectARRAY_94(dummy)

    val methods = classOf[scala2Lib.Z].getDeclaredMethods.toList ++ classOf[dottyApp.Z].getDeclaredMethods.toList
    methods.foreach { m =>
      m.getName match {
        case s"${prefix}_${suffix}" =>
          val paramClass = m.getParameterTypes()(0).getSimpleName.toLowerCase.replaceAll("""\[\]""", "ARRAY")
          assert(prefix == paramClass, s"Method `$m` erased to `$paramClass` which does not match its prefix `$prefix`")
        case _ =>
      }
    }
  }
}
