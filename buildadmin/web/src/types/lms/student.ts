import type { MediaChanges,MediaInfo } from './common'
export interface Student extends StudentForm { id:number;media:MediaInfo[];createdAt:string;updatedAt:string }
export interface StudentForm extends MediaChanges { studentCode:string;fullName:string;email:string;phone?:string;dateOfBirth?:string;gender?:string;address?:string }
export interface StudentQuery { keyword?:string;page:number;size:number }
