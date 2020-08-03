# GmusicReader
Get your Google Music statistics

## Usage
Run the following command in cmd or a terminal in the directory where ``GmusicReader.jar`` is located:  
``java -jar GmusicReader.jar <path to My Activity.json> [year] [max output]``

- <...>`` means required.
- [...]`` means optional, though to include max output you need to specifiy a year!

If the year is not provided, it will default to the current year.
If max output is not provided, it wil ldefault to 10

**NOTE: The path NEEDS to be in double quotes!**

## Getting your statistics
1. Go to [Google Takeout](https://takeout.google.com/settings/takeout)
2. Under "Select data to include" select "Deselect all"
3. Scroll down until you see "My Activity", check it to be included.
4. Under "My Activity", select "Multiple Formats", make sure it is set to JSON
5. Under "My Activity", select "All activity data included", in this menu select "Deselect all", and then only select "Google Play Music"
6. Scroll to the bottom of the page and select "Next Step
7. Set "Frequency" to "Export once"
8. Set File type to .zip, and File size to 2GB
9. Press "Create Export"
10. You will receive an email from Google shortly with a download link. This will be a ZIP file.
11. In this ZIP file you need to navigate to ``Takeout/My Activity/Google Play Music/``. You need to copy out ``My Activity.json``.
