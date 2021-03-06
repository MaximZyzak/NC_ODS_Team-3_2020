import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {HttpClient, HttpParams} from '@angular/common/http';
import {GlobalConstants} from '../../../constants/global-constants';
import {Directory} from '../../../entity/Directory';
import {AuthService} from '../../../services/AuthService';
import {DirectoryPage} from '../../../pageable/DirectoryPage';
import {RouteVariableNameConstants} from '../../../constants/route-variable-names-constants';
import {MatDialog, MatDialogConfig} from "@angular/material/dialog";
import {DirectoryInputFormModalComponent} from "./directory-input-form-modal/directory-input-form-modal.component";
import {DirectoryLogFileUpdateFormModalComponent} from "./log-file-update-form-modal/log-file-update-form-modal.component";
import {AlertBarService} from "../../../services/AlertBarService";
import {
  faBolt,
  faCheck,
  faPlus,
  faRedoAlt,
  faSignInAlt,
  faStream,
  faTimes,
  faTrashAlt
} from '@fortawesome/free-solid-svg-icons';


@Component({
  selector: 'app-directories',
  templateUrl: './directories.component.html'
})
export class DirectoriesComponent implements OnInit {

  plusIcon = faPlus;
  logsIcon = faStream;
  proceedIcon = faSignInAlt;
  deleteIcon = faTrashAlt;
  updateIcon = faRedoAlt;
  enabledIcon = faCheck;
  disabledIcon = faTimes;
  connectIcon = faBolt;

  directories: Directory[] = [];

  directoryPage: DirectoryPage;

  serverId: string;
  msg: string;

  constructor(private authService: AuthService,
              private router: Router,
              private http: HttpClient,
              private dialog: MatDialog,
              private alertBarService: AlertBarService) {
    this.serverId = localStorage.getItem(RouteVariableNameConstants.serverToDirectoryVariableName);
  }

  getDirectoriesFromPage(pageNumber: number): void {

    let params = new HttpParams()
      .set('parentId', this.serverId.toString())
      .set('page', pageNumber.toString());

    this.http.get<DirectoryPage>(GlobalConstants.apiUrl + 'api/directory/', {params}).subscribe(result => {
      console.log(result);
      this.directoryPage = result;
      this.directoryPage.number = this.directoryPage.number + 1;// In Spring pages start from 0.
      console.log(this.directoryPage);
    });
  }

  ngOnInit(): void {
    this.getDirectoriesFromPage(1);
  }

  ngOnDestroy(): void {
    this.alertBarService.resetMessage();
  }

  routeToLogs(dir: Directory): void {
    localStorage.setItem(RouteVariableNameConstants.directoryToLogsVariableName, dir.objectId);
    localStorage.removeItem(RouteVariableNameConstants.logFileToLogsVariableName);
    this.updateDirectory(dir);
    this.router.navigateByUrl('/logs');
  }

  routeToLogFiles(directory: Directory): void {
    localStorage.setItem(RouteVariableNameConstants.directoryToLogFilesVariableName, JSON.stringify(directory));
    this.router.navigateByUrl('/logFiles');
  }

  updateDirectory(dir: Directory) {
    dir.lastExistenceCheck = new Date();
    this.http.put(GlobalConstants.apiUrl + 'api/directory/update', dir).subscribe();
  }

  deleteDirectory(objectId: string): void {
    this.http.delete(GlobalConstants.apiUrl + 'api/directory/delete/' + objectId).subscribe(() => {
      let deletedDirectory = this.directoryPage.content.find(deletedElement => deletedElement.objectId == objectId);
      let index = this.directoryPage.content.indexOf(deletedDirectory);
      this.directoryPage.content.splice(index, 1);
    });
  }

  connectEnabled(dir:Directory):void{
    dir.enabled = !dir.enabled;
    dir.connectable = dir.enabled;
    this.http.put(GlobalConstants.apiUrl + 'api/directory/update', dir).subscribe();
  }

  openInsertModal() {

    const dialogConfig = new MatDialogConfig();

    dialogConfig.autoFocus = true;
    dialogConfig.width = '500px';
    dialogConfig.panelClass = 'custom-dialog-container';

    const dialogRef = this.dialog.open(DirectoryInputFormModalComponent, dialogConfig);
    dialogRef.afterClosed().subscribe(data => {
      if (data !== undefined) {
        this.alertBarService.setConfirmMessage('Directory ' + data['path'] + ' added successfully');
        this.getDirectoriesFromPage(1);
      }
    });
  }

  openUpdateModal(dir:Directory) {
    const dialogConfig = new MatDialogConfig();

    dialogConfig.autoFocus = true;
    dialogConfig.width = '500px';
    dialogConfig.panelClass = 'custom-dialog-container';
    dialogConfig.data = dir

    const dialogRef = this.dialog.open(DirectoryLogFileUpdateFormModalComponent, dialogConfig);
    dialogRef.afterClosed().subscribe(data => {
      if (data !== undefined) {
        this.alertBarService.setConfirmMessage('Log files directory ' + data['path'] + ' updated successfully');
        this.getDirectoriesFromPage(1);
      }
    });
  }
}
