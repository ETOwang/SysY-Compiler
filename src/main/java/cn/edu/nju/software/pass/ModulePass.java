package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.module.ModuleRef;


public interface ModulePass extends Pass{
   boolean runOnModule(ModuleRef module) ;
}
