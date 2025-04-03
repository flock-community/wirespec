class Wirespec < Formula
  desc "Readable contracts and typesafe wires made easy"
  homepage "https://github.com/flock-community/wirespec"
  url "https://github.com/flock-community/wirespec/archive/refs/tags/v0.13.0.tar.gz"
  sha256 "checksum"
  license "Apache-2.0"
  head "https://github.com/flock-community/wirespec.git", branch: "master"

  bottle do
    sha256 cellar: :any_skip_relocation, sonoma:  "..."
    sha256 cellar: :any_skip_relocation, arm64_sonoma:  "..."
    sha256 cellar: :any_skip_relocation, x86_64_linux:  "..."
  end

  def install
    # TODO
  end

  test do
    # TODO
  end
end
