package com.redhat.foreman;

import java.io.IOException;

import org.apache.log4j.Logger;

import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.User;
import hudson.model.Queue.Task;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.OfflineCause;

/**
 * Foreman Cloud Computer.
 */
public class ForemanComputer extends AbstractCloudComputer<ForemanSharedNode> {

    private static final Logger LOGGER = Logger.getLogger(ForemanComputer.class);

    @Override
    public void taskCompleted(Executor executor, Task task, long durationMS) {
        eagerlyReturnNode(executor);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Task task, long durationMS, Throwable problems) {
        eagerlyReturnNode(executor);
    }

    /**
     * We want to eagerly return the node to Foreman.
     * @param owner Computer.
     */
    private synchronized void eagerlyReturnNodeLater(final Computer owner) {
        Node node = owner.getNode();
        if (node instanceof ForemanSharedNode) {
            ForemanSharedNode sharedNode = (ForemanSharedNode)node;
            sharedNode.toComputer().setTemporarilyOffline(true,
                    new OfflineCause.UserCause(User.current(), "Foreman Shared Plugin offline"));
            Computer.threadPoolForRemoting.submit(new Runnable() {
                public void run() {
                    if (owner instanceof ForemanComputer) {
                        Node node = owner.getNode();
                        if (node instanceof ForemanSharedNode) {
                              try {
                                  ForemanSharedNode sharedNode = (ForemanSharedNode)node;
                                  sharedNode.terminate();
                              } catch (InterruptedException e) {
                                  LOGGER.warn(e.getMessage());
                              } catch (IOException e) {
                                  LOGGER.warn(e.getMessage());
                              }
                        }
                    }
                }
            });
        }
    }

    /**
     * Eagerly return resource to Foreman.
     * @param executor Executor.
     */
    private void eagerlyReturnNode(Executor executor) {
        eagerlyReturnNodeLater(executor.getOwner());
    }

    /**
     * Default constructor.
     * @param slave Foreman slave.
     */
    public ForemanComputer(ForemanSharedNode slave) {
        super(slave);
    }

}
