/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils;

import org.openconcerto.utils.cc.IClosure;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Holds items and give them one at a time to {@link #process(Object)}. At any time this process can
 * be put on hold by setting it to sleep. All actions in a thread modifying ({@link #put(Object)},
 * {@link #itemsDo(IClosure)}) the queue happens-before the processing of this modification.
 * 
 * @author Sylvain
 * @param <T> type of item
 */
@ThreadSafe
public abstract class DropperQueue<T> extends Thread {

    @GuardedBy("itemsLock")
    private final Deque<T> items;
    private final Lock itemsLock;
    private final Condition notEmpty;
    @GuardedBy("this")
    private boolean stop;
    @GuardedBy("this")
    private boolean sleeping;
    @GuardedBy("this")
    private boolean executing;

    /**
     * Construct a new instance.
     * 
     * @param name name of this thread.
     */
    public DropperQueue(String name) {
        super(name);
        this.items = new LinkedList<T>();
        this.stop = false;
        this.sleeping = false;
        this.executing = false;
        this.itemsLock = new ReentrantLock();
        this.notEmpty = this.itemsLock.newCondition();
    }

    // *** boolean

    /**
     * Whether this queue should stop temporarily.
     * 
     * @param b <code>true</code> to put this to sleep, <code>false</code> to wake it.
     * @return <code>true</code> if sleeping has changed.
     */
    public synchronized final boolean setSleeping(boolean b) {
        if (this.sleeping != b) {
            this.sleeping = b;
            this.signalChange();
            return true;
        } else
            return false;
    }

    public synchronized boolean isSleeping() {
        return this.sleeping;
    }

    private synchronized void signalChange() {
        this.signalChange(false);
    }

    private synchronized void signalChange(boolean signalClosure) {
        // interrompt la thread, si elle est dans le await() ou le take()
        // elle recheckera les booleens, ATTN elle peut-être en attente du lock juste après take(),
        // c'est pourquoi on efface le flag avant process()
        // en général pas besoin d'interrompre la closure, puisque tant qu'on l'exécute
        // on ne peut, ni on n'a besoin de prendre depuis la queue
        if (signalClosure || !this.isExecuting())
            this.interrupt();
    }

    private synchronized void setExecuting(boolean b) {
        this.executing = b;
    }

    private synchronized boolean isExecuting() {
        return this.executing;
    }

    private void await() throws InterruptedException {
        synchronized (this) {
            if (this.sleeping) {
                this.wait();
            }
        }
    }

    /**
     * Signal that this thread must stop indefinitely. This method interrupts
     * {@link #process(Object)}.
     * 
     * @see #die(boolean)
     */
    public final void die() {
        this.die(true);
    }

    /**
     * Signal that this thread must stop indefinitely. Once this method returns, it is guaranteed
     * that no new item will be processed, and that this thread will {@link #isDead() die}. But if
     * this thread is currently {@link #process(Object) processing} an item, then the method will
     * finish normally if :
     * <ul>
     * <li><code>mayInterruptIfRunning</code> is <code>false</code></li>
     * <li><code>mayInterruptIfRunning</code> is <code>true</code> but the {@link #interrupt()}
     * isn't checked by the implementing subclass</li>
     * </ul>
     * 
     * @param mayInterruptIfRunning <code>true</code> to interrupt while in {@link #process(Object)}
     * @see #isDying()
     */
    public synchronized final void die(boolean mayInterruptIfRunning) {
        this.stop = true;
        this.signalChange(mayInterruptIfRunning);
    }

    /**
     * Whether this queue is dying.
     * 
     * @return <code>true</code> if {@link #die(boolean)} has been called and
     *         {@link #process(Object)} is still executing.
     * @see #isDead()
     */
    public synchronized final boolean isDying() {
        return this.dieCalled() && this.isExecuting();
    }

    /**
     * Whether this queue is active.
     * 
     * @return <code>true</code> if {@link #process(Object)} isn't executed and won't ever be again.
     * @see #isDying()
     */
    public final boolean isDead() {
        // either we're dead because die() has been called, or because process() threw an Error
        return this.getState().equals(State.TERMINATED);
    }

    public synchronized final boolean dieCalled() {
        return this.stop;
    }

    // *** Run

    @Override
    public void run() {
        while (!this.dieCalled()) {
            try {
                this.await();
                final T item;
                // lockInterruptibly() to avoid taking another item after being put to sleep
                this.itemsLock.lockInterruptibly();
                try {
                    while (this.items.isEmpty())
                        this.notEmpty.await();
                    item = this.items.removeFirst();
                } finally {
                    this.itemsLock.unlock();
                }
                this.setExecuting(true);
                // we should not carry the interrupted status in process()
                // we only use it to stop waiting and check variables again, but if we're here we
                // have removed an item and must process it
                Thread.interrupted();
                process(item);
            } catch (InterruptedException e) {
                // rien a faire, on recommence la boucle
            } catch (RuntimeException e) {
                e.printStackTrace();
                // une exn s'est produite, on considère qu'on peut passer à la suite
            } finally {
                this.setExecuting(false);
            }
        }
    }

    abstract protected void process(final T item);

    // *** items

    /**
     * Adds an item to this queue. Actions in the thread prior to calling this method happen-before
     * the passed argument is {@link #process(Object) processed}.
     * 
     * @param item the item to add.
     */
    public final void put(T item) {
        this.itemsLock.lock();
        try {
            this.items.add(item);
            this.notEmpty.signal();
        } finally {
            this.itemsLock.unlock();
        }
    }

    public final void eachItemDo(final IClosure<T> c) {
        this.itemsDo(new IClosure<Collection<T>>() {
            @Override
            public void executeChecked(Collection<T> items) {
                for (final T t : items) {
                    c.executeChecked(t);
                }
            }
        });
    }

    /**
     * Allows <code>c</code> to arbitrarily modify our queue as it is locked during this method.
     * I.e. no items will be removed (passed to the closure) nor added.
     * 
     * @param c what to do with our queue.
     */
    public final void itemsDo(IClosure<? super Deque<T>> c) {
        this.itemsLock.lock();
        try {
            c.executeChecked(this.items);
            if (!this.items.isEmpty())
                this.notEmpty.signal();
        } finally {
            this.itemsLock.unlock();
        }
    }

}
