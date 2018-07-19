source 'https://rubygems.org'

git_source(:github) do |repo_name|
  repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?("/")
  "https://github.com/#{repo_name}.git"
end

gem "yard"
gem "activesupport"
gem "parser", "2.4.0.0"
gem "pry"
gem "rubocop", File.read('.rubocop-version')
gem "safe_yaml"
