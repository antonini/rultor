/**
 * Copyright (c) 2009-2013, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.base;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.rultor.spi.Instance;
import com.rultor.stateful.Lineup;
import com.rultor.stateful.Notepad;
import java.io.StringWriter;
import javax.json.Json;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;

/**
 * Enables certain amount of parallel pulses.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
@EqualsAndHashCode(of = { "origin", "lineup", "maximum" })
@Loggable(Loggable.DEBUG)
@SuppressWarnings("PMD.DoNotUseThreads")
public final class Parallel implements Instance {

    /**
     * Origin.
     */
    private final transient Instance origin;

    /**
     * List of active threads.
     */
    private final transient Notepad active;

    /**
     * Lineup.
     */
    private final transient Lineup lineup;

    /**
     * Maximum.
     */
    private final transient int maximum;

    /**
     * Public ctor.
     * @param max Maximum
     * @param lnp Lineup
     * @param atv List of active threads
     * @param instance Original instance
     * @checkstyle ParameterNumber (5 lines)
     */
    public Parallel(final int max, @NotNull final Lineup lnp,
        @NotNull final Notepad atv, @NotNull final Instance instance) {
        this.origin = instance;
        this.lineup = lnp;
        this.maximum = max;
        this.active = atv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Loggable(value = Loggable.DEBUG, limit = Integer.MAX_VALUE)
    public void pulse() throws Exception {
        final String key = this.key();
        this.lineup.exec(
            new Runnable() {
                @Override
                public void run() {
                    Parallel.this.active.add(key);
                }
            }
        );
        try {
            if (this.active.size() <= this.maximum) {
                this.origin.pulse();
            } else {
                Logger.info(
                    this,
                    "%d thread(s) running already, which is the maximum",
                    this.maximum
                );
            }
        } finally {
            this.lineup.exec(
                new Runnable() {
                    @Override
                    public void run() {
                        Parallel.this.active.remove(key);
                    }
                }
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Logger.format(
            "%s in %d thread(s) synchronized by %s and persisted by %s",
            this.origin,
            this.maximum,
            this.lineup,
            this.active
        );
    }

    /**
     * Make a nice unique name from the current thread.
     * @return Key of the thread
     */
    private String key() {
        final Thread thread = Thread.currentThread();
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer).writeStartObject()
            .write("thread", thread.getName())
            .write("id", thread.getId())
            .write("group", thread.getThreadGroup().getName())
            .writeEnd()
            .close();
        return writer.toString();
    }

}
