source 'https://rubygems.org'

git_source(:github) do |repo_name|
  repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?("/")
  "https://github.com/#{repo_name}.git"
end

gem "yard"
gem "activesupport"
gem "parser"
gem "pry"
gem "rubocop", File.read('.rubocop-version')
gem "safe_yaml"
