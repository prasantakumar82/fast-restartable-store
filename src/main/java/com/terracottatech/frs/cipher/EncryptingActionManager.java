package com.terracottatech.frs.cipher;

import java.util.concurrent.Future;

import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionManager;
import com.terracottatech.frs.log.LogRecord;

public class EncryptingActionManager implements ActionManager {

    private final ActionManager delegate;
    private final CipherManager cipherManager;

    public EncryptingActionManager(ActionManager delegate, CipherManager cipherManager) {
        this.delegate = delegate;
        this.cipherManager = cipherManager;
    }

    @Override
    public Future<Void> syncHappened(Action action) {
        return delegate.syncHappened(new EncryptedAction(action, cipherManager));
    }

    @Override
    public Future<Void> happened(Action action) {
        return delegate.happened(new EncryptedAction(action, cipherManager));
    }

    @Override
    public Action extract(LogRecord record) {
        Action action =  delegate.extract(record);
        if (action instanceof EncryptedAction) {
            return ((EncryptedAction) action).getDelegate();
        } else {
            return action;
        }
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
    }

    @Override
    public LogRecord barrierAction() {
        return delegate.barrierAction();
    }
}
