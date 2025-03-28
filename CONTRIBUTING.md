# Contributing to Wirespec

Check out ways to contribute to Wirespec:

## Feature requests

When you have an idea on how we could improve, please check
our [discussions](https://github.com/flock-community/wirespec/discussions) to see if there are similar ideas or feature
requests. If there are none, please [start](https://github.com/flock-community/wirespec/discussions/new) your feature
request as a new discussion topic. Add the title `[Feature Request] My awesome feature` and a description of what you
expect from the improvement and what the use case is.

## We ♥ pull requests

Help out the whole community by sending your merge requests and issues. Check out how to set it up:

Setup:

``` bash
# Clone the repo:
git clone https://github.com/flock-community/wirespec.git
cd wirespec

# Create a branch for your changes
git checkout -b feature/my-awesome-feature
```

Make sure everything works as expected:

``` bash
make
```

Create a Pull Request:

- At [GitHub Flock](https://github.com/flock-community/wirespec) click on fork (at the right top)

``` bash
# add fork to your remotes
git remote add fork git@github.com:<your-user>/wirespec.git

# push new branch to your fork
git push -u fork feature/my-awesome-feature
```

- Go to your fork and create a Pull Request :).

Some things that will increase the chance that your merge request is accepted:

- Write in a functional style (we use [Arrow](https://arrow-kt.io/))
- Write test scripts.
- Write a [good commit message](https://www.conventionalcommits.org/).
