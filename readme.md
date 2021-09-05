# Jetris
Java Tetris!

This project took me embarrassingly long to complete, and there may still be 
some bugs lurking in it. My goal was to adhere to the official Tetris 
guidelines as much as possible. I believe I've done that save for a few 
optional features such as T-spin detection and sound effects for various
player actions. Avid Tetris players will notice that I blatantly stole the
look and feel of [Tetr.io](https://tetr.io); an inspiration I wear proudly
on my sleeve. 

The first iteration of the program I used a pure Swing approach, which worked 
OK but lacked in performance since AWT paints the screen whenever it feels 
like. Ignoring the AWT paint mechanism and using active rendering 
(see this [Java Trail](https://docs.oracle.com/javase/tutorial/extra/fullscreen/rendering.html))
resulted in better much performance. However, this approach came at the cost of
complexity such as managing the framerate using accurate timers; dealing with 
volatile render surfaces; avoiding race-conditions between the event dispatch
thread and the main game loop, etc.

Another challenge was scalable rendering. Instead of calculating dimensions
based on the window size, which I found to be very confusing, I instead chose
to create a fixed-size `BufferedImage` to draw all of the components onto.
Then, it was a simple matter of scaling the image to fit into the window. This
way, I could pre-plan the exact relationship between components, use hardcoded
values, and reason about it all much more easily.

Many times I came across bugs because I forgot that Java arrays are objects
themselves. Assignments between array references and methods like 
`Arrays.copyOf()` do not create deep copies, which means that the programmer 
can end-up overwriting an array which they thought was a separate object.
See the constructor of `Piece(Kind, Grid)`, particularly the assignment to
`blocks` to see the implications of this; replace `deepcopy()` with 
`Arrays.copyOf()` and see the results.

Lastly, one annoying issue is platform-dependence in key event delivery. As it
is now, the game feels a bit 'rusty' because of the overly long repeat delay
when holding down a key. Changing this delay amount or working around it is very
difficult without tideous OS specific code as event delivery is inherently 
something platform-dependent. See this [question](https://stackoverflow.com/questions/7537570/eliminating-initial-keypress-delay).

Despite any issues I believe my program has many merits. Browsing online for 
example Tetris implementations I didn't see many people using the clever, if
I do say so myself, optimizations/tricks I used in my code. For example, the
use of an aggregate array to keep track of the number of filled blocks per row
which simplifies checks when clearing lines (as opposed to a nested loop). And 
of course, my favourite, the clever algorithm used to calculate the drop 
distance of a piece (see `_drop()` in `Piece`).

Aside from those, I tried to enforce strict encapsulation among classes; write
plenty of *useful* comments; and adhere to a consistent code style. Some things
I'd still like todo:
- T-spin detection
- Sound effects
- Using pre-computed forms instead of programatic rotation (see `_rotate()` in `Piece`)

I invite you to read through the code and test it. Since this was just a small
project to keep me busy over summer I won't be updating it (very) much unless 
genius strikes me or I can't sleep at night 😛