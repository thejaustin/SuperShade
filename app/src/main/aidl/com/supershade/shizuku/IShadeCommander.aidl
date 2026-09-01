package com.supershade.shizuku;

interface IShadeCommander {
    boolean exec(in String[] cmd);
    String execForOutput(in String[] cmd);
}
