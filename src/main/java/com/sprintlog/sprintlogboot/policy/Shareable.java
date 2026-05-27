package com.sprintlog.sprintlogboot.policy;

public interface Shareable {
    boolean canShare();

    String getSharTitle();
    //인터페이스는 구현하고자 하는 클래스에게 역할을 주는 기능을 함
}
