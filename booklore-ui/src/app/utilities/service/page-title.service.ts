import {inject, Injectable} from "@angular/core";
import {Title} from "@angular/platform-browser";
import {Book} from "../../book/model/book.model";

@Injectable({providedIn: 'root'})
export class PageTitleService {
    private titleService = inject(Title)

    private appName = 'BookLore'

    setBookReadingPageTitle(book: Book) {
        const title = [
            book.metadata?.title,
            book.metadata?.seriesName ? `(${book.metadata.seriesName} series)` : false,
            book.metadata?.authors?.length ? `- by ${new Intl.ListFormat('en').format(book.metadata?.authors)}` : false,
        ].filter(part => part);

        if (title.length === 0) {
            title.push((book.fileName || book.filePath)!)
        }

        this.setPageTitle(title.join(' '));
    }

    setSingleBookPageTitle() {
        this.setPageTitle('');
    }


    setLibraryPageTitle() {
        this.setPageTitle('');
    }

    setShelvePageTitle() {
        this.setPageTitle('');
    }

    setPageTitle(pageTitle: string) {
        this.titleService.setTitle([pageTitle, this.appName].join(' - '));
    }
}
