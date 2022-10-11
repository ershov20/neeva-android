General description of what your PR does, written in a way that someone six
months from now could understand what you were doing when tracking down a bug
or attempting to add new functionality

* More details about the changes
* Even more details about the changes
* Yet more details about the changes

## Checklists

### Pointers to help with review
* Figma link (if this is a UI change)
* Link to where the change is discussed (e.g. meeting notes, design docs)

### How was this tested?
- [ ] I tested this manually on `debug`
- [ ] I tested this manually on `staging` or `release`
- [ ] I added automated tests
- [ ] Existing automated tests cover some of my changes
- [ ] Existing automated tests cover all of my changes
- [ ] I checked that `@Previews` still work in Android Studio

### Manual test cases
If manually tested, describe the different scenarios you followed to ensure
that your changes work.  Make sure that you've tested as many pathways as
possible -- not just the [happy path](https://en.wikipedia.org/wiki/Happy_path).

### Screenshots and screen recordings
Images or videos showing the effect of your PR.  Examples include:
* For new Composables, add images of `@Preview`s you have added for both light
  and dark mode
* Before and after screenshots showing the effect of your styling changes
* Videos showing you performing the flow that you have changed

## Issues addressed
* Link(s) to Github issue(s) (if applicable)
