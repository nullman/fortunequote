(jde-project-file-version "1.0")
(jde-set-variables
 ;; java file header
 ;; '(jde-gen-buffer-boilerplate
 ;;   (quote
 ;;    ("/*"
 ;;     " * Geotools - OpenSource mapping toolkit"
 ;;     " * http://geotools.org"
 ;;     " * (C) 2002, Geotools Project Managment Committee (PMC)"
 ;;     " *"
 ;;     " * This library is free software; you can redistribute it and/or"
 ;;     " * modify it under the terms of the GNU Lesser General Public"
 ;;     " * License as published by the Free Software Foundation;"
 ;;     " * version 2.1 of the License."
 ;;     " *"
 ;;     " * This library is distributed in the hope that it will be useful,"
 ;;     " * but WITHOUT ANY WARRANTY; without even the implied warranty of"
 ;;     " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU"
 ;;     " * Lesser General Public License for more details."
 ;;     " *"
 ;;     " */")))
 ;; log size
 '(jde-log-max 5000)
 ;; auto-complete
 '(jde-enable-abbrev-mode nil)
 ;; source file paths
 `(jde-sourcepath
   (quote
    (,(expand-file-name "./src")
     ,(expand-file-name "./res")
     ,(expand-file-name "./gen")
     ,(expand-file-name (concat (getenv "JAVA_HOME") "/src")))))
 ;; classpath
 `(jde-global-classpath
   (quote
    (,(expand-file-name "./bin/classes")
     ,(expand-file-name "android.jar" android-mode-sdk-dir))))
 ;; startup class
 '(jde-run-application-class "com.nullware.android.fortunequote.FortuneQuote")
 '(jde-run-working-directory ".")
 ;; make program
 '(jde-make-program "ant")
 ;; make args
 '(jde-make-args "jar")
 ;; javadoc version tag template
 ;; '(jde-javadoc-version-tag-template "\"* @version $Id: prj.el,v 1.4 2003/04/23 14:28:25 kobit Exp $\"")
 ;; bracket placement style
 ;; '(jde-gen-k&r t)
 ;; have separate import for each used class
 '(jde-import-auto-collapse-imports nil)
 ;; jdk
 '(jde-compile-option-target (quote ("1.6")))
 ;; sort imports
 '(jde-import-auto-sort t)
 ;; syntax highlighting buffer parse interval
 ;; '(jde-auto-parse-buffer-interval 180)
 ;; project email address
 ;; '(user-mail-address "kylewsherman@gmail.com")
)
