package com.nastsin.akka.node.actor.timer.custom;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.nastsin.akka.common.entity.AkkaCommand;
import com.nastsin.akka.common.entity.Control;
import com.nastsin.akka.common.entity.Do;
import com.nastsin.akka.common.entity.Timeout;
import com.nastsin.akka.common.util.TestUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TimerActor extends AbstractBehavior<AkkaCommand> {

    private final String TIMER_KEY = "TimeoutKey";

    private final TimerScheduler<AkkaCommand> timer;
    private final Duration after;

    private List<Double> timing = new ArrayList<>();

    private final int id;

    private int countCommand;
    private int countTimeout;

    public TimerActor(ActorContext<AkkaCommand> context, TimerScheduler<AkkaCommand> timer, Duration after) {
        super(context);
        this.timer = timer;
        this.after = after;
        this.id = TestUtil.getId();
    }

    public static Behavior<AkkaCommand> create(Duration after) {
        return Behaviors.withTimers(timer -> Behaviors.setup(context -> new TimerActor(context, timer, after)));
    }

    @Override
    public Receive<AkkaCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Do.class, param -> {
                    getContext().getLog().debug("Do!");
                    countCommand++;
                    timer.startSingleTimer(new Timeout(System.nanoTime()), after);
                    return Behaviors.same();
                })
                .onMessage(Timeout.class, timeout -> {
                    countTimeout++;
                    double time = (double) (System.nanoTime() - timeout.timestamp) / 1000000;
                    timing.add(time);
                    getContext().getLog().debug("Timeout. Id: {}, DurationMillis: {}, TimeMillis: {}, countCommand: {}, countTimeout: {}", id,
                            after.toMillis(), time, countCommand, countTimeout);
                    return Behaviors.same();
                })
                .onMessage(Control.class, control -> {
                    control.setAnswer("Id: " + id + " countCommand: " + countCommand + " , countTimeout: " + countTimeout);
                    control.getReplayTo().tell(control);
                    control.setId(id);
                    control.setTiming(timing);
                    return Behaviors.same();
                })
                .build();
    }
}
