package com.yimeng.servicetest.service;

/**
 * 计数器接口
 */

public interface ICounter {
    /**
     * 开始计数任务
     */
    void startCount();

    /**
     * 停止计数任务：注意，这并不一定导致解绑或者停止服务，如果将这一事件通知客户端，
     * 则需要在方法逻辑中调用计数监听接口OnCountListener的onOver方法
     * @param bySelf 计数任务自动结束true，客户端主动停止false
     */
    void stopCount(boolean bySelf);

    /**
     * 暂停计数任务
     */
    void pauseCount();

    /**
     * 继续计数
     */
    void continueCount();

    /**
     * 注册计数过程监听接口
     * @param onCountListener 监听器
     */
    void registerCountListener(OnCountListener onCountListener);

    /**
     * 计数过程监听器
     */
    interface OnCountListener {
        /**
         * 计数结束
         */
        void onOver();

        /**
         * 计数变化
         * @param number 当前数字
         */
        void onCount(int number);
    }
}
